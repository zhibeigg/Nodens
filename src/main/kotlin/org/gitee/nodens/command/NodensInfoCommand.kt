package org.gitee.nodens.command

import org.bukkit.Bukkit
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.util.comparePriority
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
                    player.attributeMemory()?.mergedAllAttribute(true)?.toSortedMap { o1, o2 ->
                        comparePriority(o1.config.handlePriority, o2.config.handlePriority)
                    }?.forEach { (key, value) ->
                        val value = "${value[DigitalParser.Type.COUNT]?.joinToString("-") ?: 0} + ${value[DigitalParser.Type.PERCENT]?.joinToString("-") { (it * 100).toString() } ?: 0}%"
                        sender.sendLang("info-attribute-argument", "${key.group.name}:${key.name}", value, key.config.keys.joinToString(", "))
                    }
                }
            }
        }
    }
}