package org.gitee.nodens.api

import org.bukkit.entity.LivingEntity
import org.gitee.nodens.api.interfaces.IAttributeAPI
import org.gitee.nodens.api.interfaces.IItemAPI
import org.gitee.nodens.api.interfaces.INodensAPI
import org.gitee.nodens.api.interfaces.IReloadAPI
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.module.item.VariableRegistry
import org.gitee.nodens.util.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

object Nodens {

    @Config
    lateinit var config: ConfigFile
        private set

    @Reload(0)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        reloadConfig()
    }

    @JvmStatic
    private var api: INodensAPI? = null

    /**
     * 注册开发者接口
     */
    @JvmStatic
    fun register(api: INodensAPI) {
        this.api = api
    }

    /**
     * 获取开发者接口
     */
    @JvmStatic
    fun api(): INodensAPI {
        return api ?: error("NodensAPI has not finished loading, or failed to load!")
    }

    @JvmStatic
    fun itemAPI(): IItemAPI {
        return api().itemAPI
    }

    @JvmStatic
    fun attributeAPI(): IAttributeAPI {
        return api().attributeAPI
    }

    @JvmStatic
    fun reloadAPI(): IReloadAPI {
        return api().reloadAPI
    }

    @JvmStatic
    fun variableRegistry(): VariableRegistry {
        return api().variableRegistry
    }

    @JvmStatic
    fun getAttributeMemory(entity: LivingEntity): EntityAttributeMemory? {
        return api().getAttributeMemory(entity)
    }

    @JvmStatic
    fun getEntityAttributeMemory(entity: LivingEntity): EntityAttributeMemory? {
        return api().getEntityAttributeMemory(entity)
    }

    @JvmStatic
    fun ensureAttributeMemory(entity: LivingEntity): EntityAttributeMemory {
        return api().ensureAttributeMemory(entity)
    }

    @JvmStatic
    fun removeAttributeMemory(entity: LivingEntity, resetHealth: Boolean = true): EntityAttributeMemory? {
        return api().removeAttributeMemory(entity, resetHealth)
    }

    @JvmStatic
    fun updateAllAttributes() {
        api().updateAllAttributes()
    }

    @JvmStatic
    fun registerAttributeGroup(group: IAttributeGroup, reloadAttributes: Boolean = true): IAttributeGroup? {
        return api().registerAttributeGroup(group, reloadAttributes)
    }

    @JvmStatic
    fun unregisterAttributeGroup(groupName: String, reloadAttributes: Boolean = true): IAttributeGroup? {
        return api().unregisterAttributeGroup(groupName, reloadAttributes)
    }

    @JvmStatic
    fun getAttributeGroup(groupName: String): IAttributeGroup? {
        return api().getAttributeGroup(groupName)
    }

    @JvmStatic
    fun getAttributeGroups(): Map<String, IAttributeGroup> {
        return api().getAttributeGroups()
    }

    @JvmStatic
    fun getAttributeConfig(groupName: String, attributeName: String): AttributeConfig? {
        return api().getAttributeConfig(groupName, attributeName)
    }

    @JvmStatic
    fun reload() {
        api().reload()
    }

    @JvmStatic
    fun reloadConfig() {
        config.reload()
        consoleMessage("&e┣&7Config loaded &a√")
    }

    @JvmStatic
    fun reloadAttributes(updateEntities: Boolean = true) {
        api().reloadAttributes(updateEntities)
    }

    @JvmStatic
    fun reloadHandle() {
        api().reloadHandle()
    }

    @JvmStatic
    fun reloadItems() {
        api().reloadItems()
    }

    @JvmStatic
    fun reloadItemGroups() {
        api().reloadAPI.reloadItemGroups()
    }

    @JvmStatic
    fun reloadConditions() {
        api().reloadAPI.reloadConditions()
    }

    @JvmStatic
    fun reloadRandoms() {
        api().reloadAPI.reloadRandoms()
    }

    @JvmStatic
    fun reloadRegainTask() {
        api().reloadAPI.reloadRegainTask()
    }

    @JvmStatic
    fun reloadByWeight(weight: Int) {
        api().reloadByWeight(weight)
    }

    @JvmStatic
    fun reloadByWeights(vararg weights: Int) {
        api().reloadByWeights(*weights)
    }
}
