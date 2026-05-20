package org.gitee.nodens.api

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.api.interfaces.IAttributeAPI
import org.gitee.nodens.api.interfaces.IItemAPI
import org.gitee.nodens.api.interfaces.INodensAPI
import org.gitee.nodens.api.interfaces.IReloadAPI
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeData
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.ensureAttributeMemory
import org.gitee.nodens.module.item.VariableRegistry
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.PlatformFactory
import java.util.UUID

@RuntimeDependencies(
    RuntimeDependency(
        "!com.github.ben-manes.caffeine:caffeine:2.9.3",
        test = "!org.gitee.nodens.caffeine.cache.Caffeine",
        relocate = ["!com.github.benmanes.caffeine", "!org.gitee.nodens.caffeine"],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
        test = "!org.gitee.nodens.serialization.Serializer",
        relocate = ["!kotlin.", "!kotlin2120.", "!kotlinx.serialization.", "!org.gitee.nodens.serialization."],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1",
        test = "!org.gitee.nodens.serialization.json.Json",
        relocate = ["!kotlin.", "!kotlin2120.", "!kotlinx.serialization.", "!org.gitee.nodens.serialization."],
        transitive = false
    ),
    RuntimeDependency(
        "!com.eatthepath:fast-uuid:0.2.0",
        test = "!org.gitee.nodens.eatthepath.uuid.FastUUID",
        relocate = ["!com.eatthepath.uuid", "!org.gitee.nodens.eatthepath.uuid"],
        transitive = false
    ),
    RuntimeDependency(
        "!org.xerial.snappy:snappy-java:1.1.10.7",
        test = "!org.xerial.snappy.Snappy",
        transitive = false
    )
)
class NodensAPI: INodensAPI {

    override val itemAPI: IItemAPI
        get() = PlatformFactory.getAPI<IItemAPI>()

    override val attributeAPI: IAttributeAPI
        get() = PlatformFactory.getAPI<IAttributeAPI>()

    override val reloadAPI: IReloadAPI
        get() = PlatformFactory.getAPI<IReloadAPI>()

    override val variableRegistry: VariableRegistry
        get() = VariableRegistry

    override fun attackEntity(damageProcessor: DamageProcessor): EntityDamageByEntityEvent? {
        return damageProcessor.callDamage()
    }

    override fun addTempAttribute(entity: LivingEntity, key: String, tempAttributeData: TempAttributeData) {
        entity.attributeMemory()?.addAttribute(key, tempAttributeData)
    }

    override fun removeTempAttribute(entity: LivingEntity, key: String) {
        entity.attributeMemory()?.removeAttribute(key)
    }

    override fun getAttributeMemory(entity: LivingEntity): EntityAttributeMemory? {
        return entity.attributeMemory()
    }

    override fun getEntityAttributeMemory(entity: LivingEntity): EntityAttributeMemory? {
        return getAttributeMemory(entity)
    }

    override fun ensureAttributeMemory(entity: LivingEntity): EntityAttributeMemory {
        return entity.ensureAttributeMemory()
    }

    override fun removeAttributeMemory(entity: LivingEntity, resetHealth: Boolean): EntityAttributeMemory? {
        return EntityAttributeMemory.removeAttributeMemory(entity, resetHealth)
    }

    override fun getAttributeMemories(): Map<UUID, EntityAttributeMemory> {
        return EntityAttributeMemory.getAttributeMemories()
    }

    override fun updateAllAttributes() {
        EntityAttributeMemory.updateAllAttributeMemories()
    }

    override fun matchAttribute(attribute: String): IAttributeData? {
        return AttributeManager.matchAttribute(attribute)
    }

    override fun matchAttributes(attributes: List<String>): List<IAttributeData> {
        return attributes.mapNotNull { matchAttribute(it) }
    }

    override fun updateAttribute(entity: LivingEntity) {
        entity.attributeMemory()?.updateAttributeAsync()
    }

    override fun registerAttributeGroup(group: IAttributeGroup, reloadAttributes: Boolean): IAttributeGroup? {
        return attributeAPI.registerAttributeGroup(group, reloadAttributes)
    }

    override fun unregisterAttributeGroup(groupName: String, reloadAttributes: Boolean): IAttributeGroup? {
        return attributeAPI.unregisterAttributeGroup(groupName, reloadAttributes)
    }

    override fun getAttributeGroup(groupName: String): IAttributeGroup? {
        return attributeAPI.getAttributeGroup(groupName)
    }

    override fun getAttributeGroups(): Map<String, IAttributeGroup> {
        return attributeAPI.getAttributeGroups()
    }

    override fun getAttributeConfig(groupName: String, attributeName: String): AttributeConfig? {
        return attributeAPI.getAttributeConfig(groupName, attributeName)
    }

    override fun reload() {
        reloadAPI.reload()
    }

    override fun reloadConfig() {
        reloadAPI.reloadConfig()
    }

    override fun reloadAttributes(updateEntities: Boolean) {
        reloadAPI.reloadAttributes(updateEntities)
    }

    override fun reloadHandle() {
        reloadAPI.reloadHandle()
    }

    override fun reloadItems() {
        reloadAPI.reloadItems()
    }

    override fun reloadItemGroups() {
        reloadAPI.reloadItemGroups()
    }

    override fun reloadConditions() {
        reloadAPI.reloadConditions()
    }

    override fun reloadRandoms() {
        reloadAPI.reloadRandoms()
    }

    override fun reloadRegainTask() {
        reloadAPI.reloadRegainTask()
    }

    override fun reloadByWeight(weight: Int) {
        reloadAPI.reloadByWeight(weight)
    }

    override fun reloadByWeights(vararg weights: Int) {
        reloadAPI.reloadByWeights(*weights)
    }

}