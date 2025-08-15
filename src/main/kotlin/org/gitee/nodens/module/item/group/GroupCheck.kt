package org.gitee.nodens.module.item.group

import eos.moe.dragoncore.api.SlotAPI
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.configuration.ConfigurationSection

class GroupCheck(val key: String, config: ConfigurationSection) {

    val slots = config.getStringList("slots")

    fun check(player: Player, any: Boolean, func: ItemStack.() -> Boolean): Boolean {
        return if (any) {
            slots.any {
                val itemStack = SlotAPI.getCacheSlotItem(player, it)
                itemStack?.func() == true
            }
        } else {
            slots.all {
                val itemStack = SlotAPI.getCacheSlotItem(player, it)
                itemStack?.func() == true
            }
        }
    }
}