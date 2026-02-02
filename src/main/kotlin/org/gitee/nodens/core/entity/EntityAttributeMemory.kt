package org.gitee.nodens.core.entity

import com.github.benmanes.caffeine.cache.Caffeine
import eos.moe.dragoncore.api.SlotAPI
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.api.events.entity.NodensEntityRegainEvents
import org.gitee.nodens.api.events.player.NodensPlayerAttributeSyncEvent
import org.gitee.nodens.api.events.player.NodensPlayerAttributeUpdateEvents
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.core.*
import org.gitee.nodens.core.attribute.Health
import org.gitee.nodens.core.attribute.JavaScript
import org.gitee.nodens.core.attribute.Mapping
import org.gitee.nodens.core.attribute.Speed
import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.module.item.condition.ConditionManager
import org.gitee.nodens.util.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.service.PlatformExecutor
import taboolib.common.util.unsafeLazy
import taboolib.module.configuration.util.ReloadAwareLazy
import taboolib.platform.util.onlinePlayers
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class EntityAttributeMemory(val entity: LivingEntity) {

    companion object {

        /** 使用 ConcurrentHashMap 保证线程安全 */
        internal val entityAttributeMemoriesMap = ConcurrentHashMap<UUID, EntityAttributeMemory>()
        private var regainTask: PlatformExecutor.PlatformTask? = null

        private val dragonCoreIsEnabled by unsafeLazy { DragonCorePlugin.isEnabled }

        private val attributeDragoncoreSlots by ReloadAwareLazy(Nodens.config) { Nodens.config.getStringList("attribute-dragoncore-slots") }
        private val attributeCatch = Caffeine.newBuilder()
            .initialCapacity(200)
            .maximumSize(500)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .recordStats()
            .build<UUID, List<IAttributeData>>()

        /** 玩家最后访问时间记录，用于超时清理 */
        private val playerLastAccessTime = ConcurrentHashMap<UUID, Long>()
        /** 玩家数据超时时间（10分钟） */
        private const val PLAYER_DATA_TIMEOUT_MS = 600_000L

        @Schedule(async = false, period = 100)
        private fun schedule() {
            val now = System.currentTimeMillis()
            val iterator = entityAttributeMemoriesMap.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val memory = entry.value
                if (memory.entity is Player) {
                    val player = memory.entity as Player
                    // 检查玩家是否在线
                    if (!player.isOnline) {
                        // 检查是否超时
                        val lastAccess = playerLastAccessTime[entry.key] ?: now
                        if (now - lastAccess > PLAYER_DATA_TIMEOUT_MS) {
                            memory.entitySyncProfile.resetHealth()
                            iterator.remove()
                            playerLastAccessTime.remove(entry.key)
                        }
                    } else {
                        // 更新在线玩家的最后访问时间
                        playerLastAccessTime[entry.key] = now
                    }
                } else {
                    // 非玩家实体：检查是否有效
                    if (!memory.entity.isValid) {
                        iterator.remove()
                    }
                }
            }
        }

        @SubscribeEvent
        private fun onPlayerJoinEvent(event: PlayerJoinEvent) {
            entityAttributeMemoriesMap[event.player.uniqueId] = EntityAttributeMemory(event.player).apply {
                syncAttributeToBukkit()
            }
        }

        @SubscribeEvent
        private fun onPlayerQuitEvent(event: PlayerQuitEvent) {
            entityAttributeMemoriesMap.remove(event.player.uniqueId)?.entitySyncProfile?.apply {
                resetHealth()
            }
            playerLastAccessTime.remove(event.player.uniqueId)
        }

        @SubscribeEvent
        private fun onEntityDeathEvent(event: EntityDeathEvent) {
            if (event.entity is Player) return
            val memory = event.entity.attributeMemory()?.extendMemory ?: return
            val iterator = memory.iterator()
            while (iterator.hasNext()) {
                val entityAttributeMemory = iterator.next()
                if (entityAttributeMemory.value.deathRemove) {
                    iterator.remove()
                }
            }
            entityAttributeMemoriesMap.remove(event.entity.uniqueId)
        }

        @SubscribeEvent
        private fun deathHeal(event: NodensEntityRegainEvents.Pre) {
            if (event.processor.passive.isDead) {
                event.isCancelled = true
            }
        }

        fun LivingEntity.attributeMemory(): EntityAttributeMemory? {
            return entityAttributeMemoriesMap[uniqueId]
        }

        @Awake(LifeCycle.DISABLE)
        private fun disable() {
            entityAttributeMemoriesMap.forEach {
                it.value.entitySyncProfile.apply {
                    resetHealth()
                }
            }
        }

        @Reload(1)
        @Awake(LifeCycle.ACTIVE)
        private fun createRegain() {
            regainTask?.cancel()
            regainTask = submitAsync(period = Health.Regain.period) {
                onlinePlayers.forEach {
                    val processor = RegainProcessor(RegainProcessor.NATURAL_REASON, it, it)
                    processor.handle()
                    submit { processor.callRegain() }
                }
            }
            consoleMessage("&e┣&7RegainTask loaded &a√")
        }

        fun ItemStack.getItemAttribute(): List<IAttributeData> {
            val lore = itemMeta?.lore ?: return emptyList()
            return lore.mapNotNull { line ->
                val match = AttributeManager.matchAttribute(line)
                if (match == null) {
                    debug("&c未匹配属性: $line")
                    null
                } else {
                    debug("&a匹配属性: $line -> ${match.attributeNumber}")
                    AttributeData(match.attributeNumber, match.value)
                }
            }
        }

        fun removeCatch(entity: UUID) {
            attributeCatch.invalidate(entity)
        }
    }

    private val extendMemory = ConcurrentHashMap<String, TempAttributeData>()
    val entitySyncProfile = EntitySyncProfile(entity)

    init {
        updateAttributeAsync()
    }

    fun addAttribute(name: String, value: TempAttributeData) {
        extendMemory[name] = value
        updateAttributeAsync()
    }

    fun removeAttribute(name: String): TempAttributeData? {
        val remove = extendMemory.remove(name)
        updateAttributeAsync()
        return remove
    }

    /**
     * 获取当前生效的临时属性数据列表
     * @return 未过期的临时属性数据
     */
    fun getExtendData(): List<IAttributeData> {
        return extendMemory.values.flatMap { if (!it.closed) it.attributeData else emptyList() }
    }

    /**
     * 获取所有临时属性的键名集合
     * @return 临时属性键名集合
     */
    fun getExtendMemoryKeys(): Set<String> {
        return extendMemory.keys.toSet()
    }

    /** 计算物品属性的实际实现 */
    private fun computeItemsAttribute(): List<IAttributeData> {
        val list = mutableListOf<IAttributeData>()

        fun add(itemStack: ItemStack?, map: Map<String, String>) {
            itemStack ?: return
            if (ConditionManager.matchConditions(entity, itemStack, emptyArray(), map)) {
                list.addAll(itemStack.getItemAttribute())
            }
        }
        add(entity.equipment?.helmet, mapOf(ConditionManager.SLOT_DATA_KEY to "helmet"))
        add(entity.equipment?.chestplate, mapOf(ConditionManager.SLOT_DATA_KEY to "chestplate"))
        add(entity.equipment?.leggings, mapOf(ConditionManager.SLOT_DATA_KEY to "leggings"))
        add(entity.equipment?.boots, mapOf(ConditionManager.SLOT_DATA_KEY to "boots"))
        add(entity.equipment?.itemInMainHand, mapOf(ConditionManager.SLOT_DATA_KEY to "main-hand"))
        add(entity.equipment?.itemInOffHand, mapOf(ConditionManager.SLOT_DATA_KEY to "off-hand"))
        if (entity is Player && dragonCoreIsEnabled) {
            attributeDragoncoreSlots.forEach { id ->
                val item = SlotAPI.getCacheSlotItem(entity, id) ?: return@forEach
                add(item, mapOf(ConditionManager.SLOT_DATA_KEY to "dragoncore", ConditionManager.SLOT_IDENTIFY_KEY to id))
            }
        }
        return list
    }

    fun getItemsAttribute(ignoreCache: Boolean = false): List<IAttributeData> {
        return if (ignoreCache) {
            val result = computeItemsAttribute()
            attributeCatch.put(entity.uniqueId, result)
            result
        } else {
            attributeCatch.get(entity.uniqueId) { computeItemsAttribute() }!!
        }
    }

    fun updateAttributeAsync() {
        if (entity.isDead) return
        val event = NodensPlayerAttributeUpdateEvents.Pre(this)
        if (event.call()) {
            // 同步清理过期的临时属性，避免并发问题
            extendMemory.entries.removeIf { it.value.closed }
            // 失效缓存
            removeCatch(entity.uniqueId)
            // 同步属性到 Bukkit
            syncAttributeToBukkit()
            // 触发后置事件
            NodensPlayerAttributeUpdateEvents.Post(this@EntityAttributeMemory).call()
        }
    }

    fun syncAttributeToBukkit() {
        if (entity.isDead) return
        val attributeData = mergedAllAttribute()
        ensureSync {
            val event = NodensPlayerAttributeSyncEvent.Pre(entitySyncProfile, attributeData)
            if (event.call()) {
                Health.Max.sync(entitySyncProfile, attributeData[Health.Max] ?: emptyMap())
                Speed.Attack.sync(entitySyncProfile, attributeData[Speed.Attack] ?: emptyMap())
                Speed.Move.sync(entitySyncProfile, attributeData[Speed.Move] ?: emptyMap())
                JavaScript.numbers.values.forEach {
                    try {
                        it.sync(entitySyncProfile, attributeData[it] ?: emptyMap())
                    } catch (_: Throwable) {
                    }
                }
                entitySyncProfile.applyModifiers()
                entitySyncProfile.resetHealth()
                NodensPlayerAttributeSyncEvent.Post(entitySyncProfile, attributeData).call()
            }
        }
    }

    fun mergedAllAttribute(isTransferMap: Boolean = true): Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>> {
        val mergeData = hashMapOf<IAttributeGroup.Number, MutableList<DigitalParser.Value>>()

        // 直接遍历 extendMemory，避免 flatMap 创建临时列表
        for (temp in extendMemory.values) {
            if (!temp.closed) {
                for (attr in temp.attributeData) {
                    mergeData.getOrPut(attr.attributeNumber) { mutableListOf() }.add(attr.value)
                }
            }
        }

        // 直接遍历 itemsData，避免 + 操作创建临时列表
        for (attr in getItemsAttribute()) {
            mergeData.getOrPut(attr.attributeNumber) { mutableListOf() }.add(attr.value)
        }

        // 使用 List 版本的 mergeValues，避免 spread operator
        val result = hashMapOf<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>()
        for ((number, values) in mergeData) {
            result[number] = mergeValues(values)
        }

        return if (isTransferMap) {
            transferMap(result)
        } else {
            result
        }
    }

    fun mergedExtendAttribute(isTransferMap: Boolean = true): Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>> {
        val mergeData = hashMapOf<IAttributeGroup.Number, MutableList<DigitalParser.Value>>()

        // 直接遍历 extendMemory，避免 flatMap 创建临时列表
        for (temp in extendMemory.values) {
            if (!temp.closed) {
                for (attr in temp.attributeData) {
                    mergeData.getOrPut(attr.attributeNumber) { mutableListOf() }.add(attr.value)
                }
            }
        }

        // 使用 List 版本的 mergeValues
        val result = hashMapOf<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>()
        for ((number, values) in mergeData) {
            result[number] = mergeValues(values)
        }

        return if (isTransferMap) {
            transferMap(result)
        } else {
            result
        }
    }

    fun mergedAttribute(attribute: IAttributeGroup.Number, isTransferMap: Boolean = true): Map<DigitalParser.Type, DoubleArray> {
        val mergeData = hashMapOf<IAttributeGroup.Number, MutableList<DigitalParser.Value>>()

        // 直接遍历并只收集需要的属性
        for (temp in extendMemory.values) {
            if (!temp.closed) {
                for (attr in temp.attributeData) {
                    val number = attr.attributeNumber
                    if (number === attribute || number is Mapping.MappingAttribute) {
                        mergeData.getOrPut(number) { mutableListOf() }.add(attr.value)
                    }
                }
            }
        }

        for (attr in getItemsAttribute()) {
            val number = attr.attributeNumber
            if (number === attribute || number is Mapping.MappingAttribute) {
                mergeData.getOrPut(number) { mutableListOf() }.add(attr.value)
            }
        }

        // 使用 List 版本的 mergeValues
        val result = hashMapOf<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>()
        for ((number, values) in mergeData) {
            result[number] = mergeValues(values)
        }

        return if (isTransferMap) {
            transferMap(result)[attribute] ?: emptyMap()
        } else {
            result[attribute] ?: emptyMap()
        }
    }

    fun transferMap(map: Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>): Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>> {
        val mutableMap = map.toMutableMap()
        for ((key, value) in map) {
            if (key is Mapping.MappingAttribute) {
                for ((attrKey, attrValue) in key.getAttributes(entity, value)) {
                    // 使用 mergeMaps 避免创建临时 DigitalParser.Value 对象
                    mutableMap[attrKey] = mergeMaps(attrValue, mutableMap[attrKey])
                }
            }
        }
        return mutableMap
    }

    fun getCombatPower(): Map<IAttributeGroup.Number, Double> {
        return mergedAllAttribute(true).mapValues { (key, value) ->
            key.combatPower(value)
        }
    }

    fun mergedAllAttribute(data: List<IAttributeData>, isTransferMap: Boolean = true): Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>> {
        val mergeData = hashMapOf<IAttributeGroup.Number, MutableList<DigitalParser.Value>>()

        for (attr in data) {
            mergeData.getOrPut(attr.attributeNumber) { mutableListOf() }.add(attr.value)
        }

        val result = hashMapOf<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>()
        for ((number, values) in mergeData) {
            result[number] = mergeValues(values)
        }

        return if (isTransferMap) {
            transferMap(result)
        } else {
            result
        }
    }

    fun getCombatPower(data: List<IAttributeData>): Map<IAttributeGroup.Number, Double> {
        return mergedAllAttribute(data, true).mapValues { (key, value) ->
            key.combatPower(value)
        }
    }
}