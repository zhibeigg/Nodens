package org.gitee.nodens.command

import org.bukkit.entity.Player
import org.gitee.nodens.core.reload.ReloadAPI
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.expansion.createHelper

@CommandHeader("Nodens", ["no"], "Nodens属性插件主指令")
object NodensCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val reload = subCommandExec<ProxyCommandSender> {
        ReloadAPI.reload()
        sender.sendMessage("Nodens重载成功")
    }

    @CommandBody
    val item = NodensItemCommand

    @CommandBody
    val info = NodensInfoCommand

    @CommandBody
    val source = NodensSourceCommand

    @CommandBody
    val test = subCommand {
        exec<Player> {

        }
    }
}