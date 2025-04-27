package org.gitee.nodens.command

import org.bukkit.entity.Player
import org.gitee.nodens.module.ui.ItemConfigManagerUI
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.subCommand

@CommandHeader("item", description = "Nodens属性插件物品指令")
object NodensItemCommand {

    @CommandBody
    val manager = subCommand {
        exec<Player> {
            ItemConfigManagerUI(sender)
        }
    }
}