package org.gitee.nodens.api.interfaces

import org.bukkit.inventory.ItemStack
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

    /**
     * 添加耐久（不会大于最大值） 需要同步
     * @return 新耐久
     * */
    fun addDurability(itemStack: ItemStack, durability: Int): Int

    /**
     * 减少耐久（不会大于最大值） 需要同步
     * @return 新耐久
     * */
    fun takeDurability(itemStack: ItemStack, durability: Int): Int

    /**
     * 设置耐久（不会大于最大值） 需要同步
     * @return 新耐久
     * */
    fun setDurability(itemStack: ItemStack, durability: Int): Int

    /**
     * 获取耐久
     * @return 耐久
     * */
    fun getDurability(itemStack: ItemStack): Int

    /**
     * 获取最大耐久
     * @return 最大耐久
     * */
    fun getMaxDurability(itemStack: ItemStack): Int
}