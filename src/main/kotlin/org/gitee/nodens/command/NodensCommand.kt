package org.gitee.nodens.command

import org.bukkit.entity.Player
import org.gitee.nodens.common.FastMatchingMap
import org.gitee.nodens.core.AttributeManager.ATTRIBUTE_MATCHING_MAP
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.core.reload.ReloadAPI
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.subCommandExec
import taboolib.common.platform.function.info
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
            sender.inventory.itemInMainHand.itemMeta!!.lore!!.forEach {
                info(ATTRIBUTE_MATCHING_MAP.getMatchResult(it)?.remain)
            }
        }
    }
}