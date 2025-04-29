package org.gitee.nodens.api.events.item

import org.bukkit.inventory.ItemStack
import taboolib.platform.type.BukkitProxyEvent

class NodensItemUpdateEvents {

    class Pre(val old: ItemStack, val new: ItemStack) : BukkitProxyEvent()

    class Post(val old: ItemStack, val new: ItemStack) : BukkitProxyEvent() {
        override val allowCancelled: Boolean
            get() = false
    }
}