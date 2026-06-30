package org.gitee.nodens.api

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.api.result.RegisterResult
import org.gitee.nodens.api.result.ReloadResult
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
        addTempAttribute(entity, key, tempAttributeData, createIfAbsent = false)
    }

    override fun addTempAttribute(entity: LivingEntity, key: String, tempAttributeData: TempAttributeData, createIfAbsent: Boolean) {
        val memory = if (createIfAbsent) entity.ensureAttributeMemory() else entity.attributeMemory()
        memory?.addAttribute(key, tempAttributeData)
    }

    override fun addTempAttributeOrCreateMemory(entity: LivingEntity, key: String, tempAttributeData: TempAttributeData) {
        addTempAttribute(entity, key, tempAttributeData, createIfAbsent = true)
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
        entity.attributeMemory()?.updateAttribute()
    }

    override fun registerAttributeGroup(group: IAttributeGroup, reloadAttributes: Boolean): IAttributeGroup? {
        return attributeAPI.registerAttributeGroup(group, reloadAttributes)
    }

    override fun registerAttributeGroup(
        group: IAttributeGroup,
        configs: Map<String, AttributeRegistrationConfig>,
        reloadAttributes: Boolean,
    ): RegisterResult {
        return attributeAPI.registerAttributeGroup(group, configs, reloadAttributes)
    }

    override fun registerAttributeGroupResult(group: IAttributeGroup, reloadAttributes: Boolean): RegisterResult {
        return attributeAPI.registerAttributeGroupResult(group, reloadAttributes)
    }

    override fun unregisterAttributeGroup(groupName: String, reloadAttributes: Boolean): IAttributeGroup? {
        return attributeAPI.unregisterAttributeGroup(groupName, reloadAttributes)
    }

    override fun unregisterAttributeGroupResult(groupName: String, reloadAttributes: Boolean): RegisterResult {
        return attributeAPI.unregisterAttributeGroupResult(groupName, reloadAttributes)
    }

    override fun rebuildAttributeMatchingMap(): ReloadResult {
        return attributeAPI.rebuildAttributeMatchingMap()
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

    override fun reloadResult(): ReloadResult {
        return reloadAPI.reloadResult()
    }

    override fun reloadConfig() {
        reloadAPI.reloadConfig()
    }

    override fun reloadConfigResult(): ReloadResult {
        return reloadAPI.reloadConfigResult()
    }

    override fun reloadAttributes(updateEntities: Boolean) {
        reloadAPI.reloadAttributes(updateEntities)
    }

    override fun reloadAttributesResult(updateEntities: Boolean): ReloadResult {
        return reloadAPI.reloadAttributesResult(updateEntities)
    }

    override fun reloadHandle() {
        reloadAPI.reloadHandle()
    }

    override fun reloadHandleResult(): ReloadResult {
        return reloadAPI.reloadHandleResult()
    }

    override fun reloadItems() {
        reloadAPI.reloadItems()
    }

    override fun reloadItemsResult(): ReloadResult {
        return reloadAPI.reloadItemsResult()
    }

    override fun reloadItemGroups() {
        reloadAPI.reloadItemGroups()
    }

    override fun reloadItemGroupsResult(): ReloadResult {
        return reloadAPI.reloadItemGroupsResult()
    }

    override fun reloadConditions() {
        reloadAPI.reloadConditions()
    }

    override fun reloadConditionsResult(): ReloadResult {
        return reloadAPI.reloadConditionsResult()
    }

    override fun reloadFormulas() {
        reloadAPI.reloadFormulas()
    }

    override fun reloadFormulasResult(): ReloadResult {
        return reloadAPI.reloadFormulasResult()
    }

    override fun reloadRegainTask() {
        reloadAPI.reloadRegainTask()
    }

    override fun reloadRegainTaskResult(): ReloadResult {
        return reloadAPI.reloadRegainTaskResult()
    }

    override fun reloadByWeight(weight: Int) {
        reloadAPI.reloadByWeight(weight)
    }

    override fun reloadByWeightResult(weight: Int): ReloadResult {
        return reloadAPI.reloadByWeightResult(weight)
    }

    override fun reloadByWeights(vararg weights: Int) {
        reloadAPI.reloadByWeights(*weights)
    }

    override fun reloadByWeightsResult(vararg weights: Int): ReloadResult {
        return reloadAPI.reloadByWeightsResult(*weights)
    }

    override fun registerReloadHook(owner: String, weight: Int, function: Runnable): RegisterResult {
        return reloadAPI.registerReloadHook(owner, weight, function)
    }

    override fun unregisterReloadHooks(owner: String): RegisterResult {
        return reloadAPI.unregisterReloadHooks(owner)
    }

    override fun registerDamageFormulaProvider(id: String, priority: Int, provider: DamageFormulaProvider): RegisterResult {
        return DamageProcessor.registerDamageFormulaProvider(id, priority, provider)
    }

    override fun unregisterDamageFormulaProvider(id: String): RegisterResult {
        return DamageProcessor.unregisterDamageFormulaProvider(id)
    }

}