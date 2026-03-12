package org.gitee.nodens.command

import org.bukkit.Bukkit
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common5.format
import taboolib.module.lang.sendLang
import taboolib.platform.util.sendLang

object NodensSourceCommand {

    @CommandBody
    val listen = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = Bukkit.getPlayer(ctx["player"]) ?: return@exec
                val memory = player.attributeMemory() ?: run {
                    sender.sendLang("source-empty")
                    return@exec
                }
                val keys = memory.getExtendMemoryKeys()
                if (keys.isEmpty()) {
                    sender.sendLang("source-empty")
                    return@exec
                }
                sender.sendLang("source-title", player.name)
                keys.forEach { key ->
                    sender.sendLang("source-key", key)
                }
            }
        }
    }
}
