package org.gitee.nodens.api.interfaces

import org.gitee.nodens.api.result.RegisterResult
import org.gitee.nodens.api.result.ReloadResult

interface IReloadAPI {

    /**
     * 重载插件全部模块。
     * */
    fun reload()

    fun reloadResult(): ReloadResult

    /**
     * 重载主配置 config.yml。
     * */
    fun reloadConfig()

    fun reloadConfigResult(): ReloadResult

    /**
     * 重载属性配置、属性匹配表与动态属性。
     *
     * @param updateEntities 是否同步刷新当前已缓存实体的属性
     * */
    fun reloadAttributes(updateEntities: Boolean = true)

    fun reloadAttributesResult(updateEntities: Boolean = true): ReloadResult

    /**
     * 重载 handle.yml 中的伤害/恢复脚本。
     * */
    fun reloadHandle()

    fun reloadHandleResult(): ReloadResult

    /**
     * 重载 items 目录中的物品配置。
     * */
    fun reloadItems()

    fun reloadItemsResult(): ReloadResult

    /**
     * 重载 group.yml 中的物品组配置。
     * */
    fun reloadItemGroups()

    fun reloadItemGroupsResult(): ReloadResult

    /**
     * 重载物品条件关键字匹配表。
     * */
    fun reloadConditions()

    fun reloadConditionsResult(): ReloadResult

    /**
     * 重载 formulas 目录中的公式配置。
     * */
    fun reloadFormulas()

    fun reloadFormulasResult(): ReloadResult

    /**
     * 重载自然恢复定时任务。
     * */
    fun reloadRegainTask()

    fun reloadRegainTaskResult(): ReloadResult

    /**
     * 执行指定权重的重载函数。
     * */
    fun reloadByWeight(weight: Int)

    fun reloadByWeightResult(weight: Int): ReloadResult

    /**
     * 执行多个指定权重的重载函数。
     * */
    fun reloadByWeights(vararg weights: Int)

    fun reloadByWeightsResult(vararg weights: Int): ReloadResult

    /**
     * 注册长期重载 Hook。
     */
    fun registerReloadHook(owner: String, weight: Int, function: Runnable): RegisterResult

    /**
     * 注销指定 owner 的全部长期重载 Hook。
     */
    fun unregisterReloadHooks(owner: String): RegisterResult
}
