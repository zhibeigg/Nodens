package org.gitee.nodens.command

import org.bukkit.Bukkit
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.bool
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendLang

object NodensInfoCommand {

    @CommandBody
    val attribute = subCommand {
        player {
            bool("transferMap") {
                exec<ProxyCommandSender> {
                    val player = Bukkit.getPlayer(ctx["player"]) ?: return@exec
                    sender.sendLang("info-attribute")
                    player.attributeMemory()?.mergedAllAttribute(true)?.forEach { (key, value) ->
                        val value = "${value[DigitalParser.Type.COUNT]?.joinToString("-")} + ${value[DigitalParser.Type.PERCENT]?.joinToString("-")}%"
                        sender.sendLang("info-attribute-argument", "${key.group.name}:${key.name}", value, key.config.keys.joinToString(", "))
                    }
                }
            }
        }
    }
}