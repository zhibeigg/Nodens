package org.gitee.nodens.core.entity

import eos.moe.dragoncore.api.SlotAPI
import kotlinx.coroutines.launch
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.api.NodensAPI.Companion.pluginScope
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
import org.gitee.nodens.util.ConfigLazy
import org.gitee.nodens.util.ensureSync
import org.gitee.nodens.util.mergeValues
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.chat.colored
import taboolib.platform.util.onlinePlayers
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class EntityAttributeMemory(val entity: LivingEntity) {

    companion object {

        internal val entityAttributeMemoriesMap = hashMapOf<UUID, EntityAttributeMemory>()
        private var regainTask: PlatformExecutor.PlatformTask? = null

        private val attributeDragoncoreSlots by ConfigLazy(Nodens.config) { getStringList("attribute-dragoncore-slots") }

        @Schedule(async = false, period = 1)
        private fun schedule() {
            val iterator = entityAttributeMemoriesMap.iterator()
            while (iterator.hasNext()) {
                val entityAttributeMemory = iterator.next()
                if (entityAttributeMemory.value.entity is Player) continue
                if (!entityAttributeMemory.value.entity.isValid) {
                    iterator.remove()
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
            info("&e┣&7RegainTask loaded &a√".colored())
        }

        fun ItemStack.getItemAttribute(): List<IAttributeData> {
            return itemMeta?.lore?.mapNotNull {
                val match = AttributeManager.matchAttribute(it) ?: return@mapNotNull null
                AttributeData(match.attributeNumber, match.value)
            } ?: emptyList()
        }
    }

    init {
        updateAttributeAsync()
    }

    private val extendMemory = ConcurrentHashMap<String, TempAttributeData>()
    val entitySyncProfile = EntitySyncProfile(entity)

    fun addAttribute(name: String, value: TempAttributeData) {
        extendMemory[name] = value
        updateAttributeAsync()
    }

    fun removeAttribute(name: String): TempAttributeData? {
        val remove = extendMemory.remove(name)
        updateAttributeAsync()
        return remove
    }

    fun getItemsAttribute(): List<IAttributeData> {
        val list = mutableListOf<IAttributeData>()
        entity.equipment?.helmet?.getItemAttribute()?.also { list.addAll(it) }
        entity.equipment?.chestplate?.getItemAttribute()?.also { list.addAll(it) }
        entity.equipment?.leggings?.getItemAttribute()?.also { list.addAll(it) }
        entity.equipment?.boots?.getItemAttribute()?.also { list.addAll(it) }
        entity.equipment?.itemInMainHand?.getItemAttribute()?.also { list.addAll(it) }
        entity.equipment?.itemInOffHand?.getItemAttribute()?.also { list.addAll(it) }
        if (entity is Player) {
            attributeDragoncoreSlots.forEach { id ->
                list.addAll(SlotAPI.getCacheSlotItem(entity, id).getItemAttribute())
            }
        }
        return list
    }

    fun updateAttributeAsync() {
        if (entity.isDead) return
        val event = NodensPlayerAttributeUpdateEvents.Pre(this)
        if (event.call()) {
            pluginScope.launch {
                val iterator = extendMemory.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.value.closed) {
                        iterator.remove()
                    }
                }
            }.invokeOnCompletion {
                syncAttributeToBukkit()
                NodensPlayerAttributeUpdateEvents.Post(this@EntityAttributeMemory).call()
            }
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
        val extData = extendMemory.values.flatMap { if (!it.closed) it.attributeData else emptyList() }
        val itemsData = getItemsAttribute()
        val mergeData = hashMapOf<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>()
        (extData + itemsData).groupBy { it.attributeNumber }.forEach { (number, list) ->
            mergeData[number] = mergeValues(*list.map { it.value }.toTypedArray())
        }
        return if (isTransferMap) {
            transferMap(mergeData)
        } else {
            mergeData
        }
    }

    fun mergedExtendAttribute(isTransferMap: Boolean = true): Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>> {
        val extData = extendMemory.values.flatMap { if (!it.closed) it.attributeData else emptyList() }
        val mergeData = hashMapOf<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>()
        extData.groupBy { it.attributeNumber }.forEach { (number, list) ->
            mergeData[number] = mergeValues(*list.map { tempAttributeData ->
                tempAttributeData.value
            }.toTypedArray())
        }
        return if (isTransferMap) {
            transferMap(mergeData)
        } else {
            mergeData
        }
    }

    fun mergedAttribute(attribute: IAttributeGroup.Number, isTransferMap: Boolean = true): Map<DigitalParser.Type, DoubleArray> {
        val extData = extendMemory.values.flatMap { if (!it.closed) it.attributeData else emptyList() }
        val itemsData = getItemsAttribute()
        val mergeData = hashMapOf<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>()
        (extData + itemsData).groupBy { it.attributeNumber }.forEach { (number, list) ->
            if (number is Mapping.MappingAttribute) {
                mergeData[number] = mergeValues(*list.map { it.value }.toTypedArray())
            }
        }
        return if (isTransferMap) {
            transferMap(mergeData)[attribute] ?: emptyMap()
        } else {
            mergeData[attribute] ?: emptyMap()
        }
    }

    fun transferMap(map: Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>): Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>> {
        val mutableMap = map.toMutableMap()
        map.forEach { (key, value) ->
            if (key is Mapping.MappingAttribute) {
                mutableMap.remove(key)
                key.getAttributes(entity, value).forEach {
                    mutableMap[it.key] = mergeValues(
                        *it.value.map { entry -> DigitalParser.Value(entry.key, entry.value) }.toTypedArray(),
                        *mutableMap[it.key]?.map { entry -> DigitalParser.Value(entry.key, entry.value) }?.toTypedArray() ?: emptyArray()
                    )
                }
            }
        }
        return mutableMap
    }
}