package org.gitee.nodens.api.events.player

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class NodensPlayerExpChangeEvents {

    class Pre(val player: Player, val amount: Int, var addon: Int) : BukkitProxyEvent()

    class Post(val player: Player, val amount: Int, val addon: Int) : BukkitProxyEvent() {
        override val allowCancelled: Boolean
            get() = false
    }
}