package org.gitee.nodens.api.interfaces

import org.gitee.nodens.module.item.IItemGenerator
import org.gitee.nodens.module.item.ItemConfig

interface IItemAPI {

    /**
     * 获取Item配置
     * */
    fun getItemConfig(key: String): ItemConfig?

    /**
     * 获得生成器
     * */
    fun getItemGenerator(): IItemGenerator

}