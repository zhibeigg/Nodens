package org.gitee.nodens.command

import org.bukkit.entity.Player
import org.gitee.nodens.core.reload.ReloadAPI
import org.gitee.nodens.util.CONTEXT_TAG
import org.gitee.nodens.util.compress
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common.platform.function.info
import taboolib.expansion.createHelper
import taboolib.module.nms.getItemTag

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
            val tag = sender.inventory.itemInMainHand.getItemTag()
            val value = tag[CONTEXT_TAG]!!.asString()
            val new = compress(value)
            tag["test"] = new
            tag.saveTo(sender.inventory.itemInMainHand)
            info("压缩前大小 ${value.toByteArray(Charsets.UTF_8).size}")
            info("压缩后大小 ${new.size}")
        }
    }
}