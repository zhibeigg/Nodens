package org.gitee.nodens.api.interfaces

interface IReloadAPI {

    /**
     * 重载插件全部模块。
     * */
    fun reload()

    /**
     * 重载主配置 config.yml。
     * */
    fun reloadConfig()

    /**
     * 重载属性配置、属性匹配表与动态属性。
     *
     * @param updateEntities 是否同步刷新当前已缓存实体的属性
     * */
    fun reloadAttributes(updateEntities: Boolean = true)

    /**
     * 重载 handle.yml 中的伤害/恢复脚本。
     * */
    fun reloadHandle()

    /**
     * 重载 items 目录中的物品配置。
     * */
    fun reloadItems()

    /**
     * 重载 group.yml 中的物品组配置。
     * */
    fun reloadItemGroups()

    /**
     * 重载物品条件关键字匹配表。
     * */
    fun reloadConditions()

    /**
     * 重载 randoms 目录中的随机动作配置。
     * */
    fun reloadRandoms()

    /**
     * 重载自然恢复定时任务。
     * */
    fun reloadRegainTask()

    /**
     * 执行指定权重的重载函数。
     * */
    fun reloadByWeight(weight: Int)

    /**
     * 执行多个指定权重的重载函数。
     * */
    fun reloadByWeights(vararg weights: Int)
}
