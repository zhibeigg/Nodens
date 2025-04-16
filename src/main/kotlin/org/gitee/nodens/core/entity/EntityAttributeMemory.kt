package org.gitee.nodens.core.entity

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.*
import org.gitee.nodens.core.attribute.Health
import org.gitee.nodens.core.attribute.Mapping
import org.gitee.nodens.core.reload.Reload
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

        private val entityAttributeMemoriesMap = hashMapOf<UUID, EntityAttributeMemory>()
        private var regainTask: PlatformExecutor.PlatformTask? = null

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
            event.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.let { instance ->
                instance.modifiers.forEach {
                    instance.removeModifier(it)
                }
            }
            entityAttributeMemoriesMap[event.player.uniqueId] = EntityAttributeMemory(event.player).apply {
                syncAttributeToBukkit()
            }
        }

        @SubscribeEvent
        private fun onPlayerQuitEvent(event: PlayerQuitEvent) {
            entityAttributeMemoriesMap.remove(event.player.uniqueId)?.entitySyncProfile?.apply {
                clearModifiers()
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

        fun LivingEntity.attributeMemory(): EntityAttributeMemory? {
            return entityAttributeMemoriesMap[uniqueId]
        }

        @Awake(LifeCycle.DISABLE)
        private fun disable() {
            entityAttributeMemoriesMap.forEach {
                it.value.entitySyncProfile.apply {
                    clearModifiers()
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
                    val attributeData = it.attributeMemory()?.mergedAllAttribute() ?: return@forEach
                    val regain = Health.Regain.getRegain(it, attributeData[Health.Regain] ?: return@forEach)
                    submit {
                        it.health = (it.health + regain).coerceAtMost(it.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: it.maxHealth)
                    }
                }
            }
            info("&e┣&7RegainTask loaded &a√".colored())
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

    fun ItemStack.getItemAttribute(): List<IAttributeData>? {
        return itemMeta?.lore?.mapNotNull {
            val match = AttributeManager.matchAttribute(it) ?: return@mapNotNull null
            AttributeData(match.attributeNumber, match.value)
        }
    }

    fun getItemsAttribute(): List<IAttributeData> {
        val list = mutableListOf<IAttributeData>()
        entity.equipment?.helmet?.getItemAttribute()?.also { list.addAll(it) }
        entity.equipment?.chestplate?.getItemAttribute()?.also { list.addAll(it) }
        entity.equipment?.leggings?.getItemAttribute()?.also { list.addAll(it) }
        entity.equipment?.boots?.getItemAttribute()?.also { list.addAll(it) }
        entity.equipment?.itemInMainHand?.getItemAttribute()?.also { list.addAll(it) }
        entity.equipment?.itemInOffHand?.getItemAttribute()?.also { list.addAll(it) }
        return list
    }

    fun updateAttributeAsync() {
        submitAsync {
            val iterator = extendMemory.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.closed) {
                    iterator.remove()
                }
            }
            syncAttributeToBukkit()
        }
    }

    fun syncAttributeToBukkit() {
        ensureSync {
            val attributeData = mergedAllAttribute()
            entitySyncProfile.clearModifiers()
            attributeData.forEach {
                it.key.sync(entitySyncProfile, it.value)
            }
            entitySyncProfile.applyModifiers()
            entitySyncProfile.resetHealth()
        }
    }

    fun mergedAllAttribute(isTransferMap: Boolean = true): Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>> {
        val extData = extendMemory.values.mapNotNull { if (!it.closed) it.attributeData else null }
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
        val data = extendMemory.values
        val mergeData = hashMapOf<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>()
        data.groupBy { it.attributeData.attributeNumber }.forEach { (number, list) ->
            mergeData[number] = mergeValues(*list.mapNotNull { tempAttributeData ->
                if (!tempAttributeData.closed) {
                    tempAttributeData.attributeData.value
                } else {
                    null
                }
            }.toTypedArray())
        }
        return if (isTransferMap) {
            transferMap(mergeData)
        } else {
            mergeData
        }
    }

    fun mergedAttribute(attribute: IAttributeGroup.Number, isTransferMap: Boolean = true): Map<DigitalParser.Type, DoubleArray> {
        val extData = extendMemory.values.mapNotNull { if (!it.closed) it.attributeData else null }
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