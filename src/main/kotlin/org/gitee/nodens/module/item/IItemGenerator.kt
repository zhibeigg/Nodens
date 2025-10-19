package org.gitee.nodens.module.item

import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface IItemGenerator {

    /**
     * 生成物品堆
     * @param itemConfig 生成的物品堆配置
     * @param amount 物品堆的数量（1-64）
     * @param player 脚本执行玩家（null=[ConsoleCommandSender]）
     * @param map 额外数据
     * @return 物品堆
     * */
    fun generate(itemConfig: ItemConfig, amount: Int, player: Player?, map: Map<String, Any> = emptyMap(), callEvent: Boolean = true): ItemStack

    /**
     * 重新刷新物品
     * @param player 脚本执行玩家（null=[ConsoleCommandSender]）
     * @param itemStack 需要重构的物品堆
     * @return 重构后的物品堆
     * */
    fun update(player: Player?, itemStack: ItemStack): ItemStack?
}