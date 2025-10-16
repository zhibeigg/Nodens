package org.gitee.nodens.command

import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.util.comparePriority
import org.gitee.nodens.util.context
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.bool
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.info
import taboolib.common5.cbool
import taboolib.common5.format
import taboolib.module.lang.sendLang
import taboolib.platform.util.sendLang

object NodensInfoCommand {

    @CommandBody
    val attribute = subCommand {
        player {
            bool("transferMap") {
                exec<ProxyCommandSender> {
                    val player = Bukkit.getPlayer(ctx["player"]) ?: return@exec
                    sender.sendLang("info-attribute")
                    player.attributeMemory()?.mergedAllAttribute(ctx["transferMap"].cbool)?.toSortedMap { o1, o2 ->
                        comparePriority(o1.config.handlePriority, o2.config.handlePriority)
                    }?.forEach { (key, value) ->
                        val value = "${value[DigitalParser.Type.COUNT]?.joinToString("-") ?: 0} + ${value[DigitalParser.Type.PERCENT]?.joinToString("-") { (it * 100).format(1).toString() } ?: 0}%"
                        sender.sendLang("info-attribute-argument", "${key.group.name}:${key.name}", value, key.config.keys.joinToString(", "))
                    }
                }
            }
        }
    }

    @CommandBody
    val entity = subCommand {
        bool("transferMap") {
            exec<Player> {
                val entity = sender.getNearbyEntities(1.0, 1.0, 1.0).first() as LivingEntity
                sender.sendLang("info-attribute")
                entity.attributeMemory()?.mergedAllAttribute(ctx["transferMap"].cbool)?.toSortedMap { o1, o2 ->
                    comparePriority(o1.config.handlePriority, o2.config.handlePriority)
                }?.forEach { (key, value) ->
                    val value = "${value[DigitalParser.Type.COUNT]?.joinToString("-") ?: 0} + ${value[DigitalParser.Type.PERCENT]?.joinToString("-") { (it * 100).format(1).toString() } ?: 0}%"
                    sender.sendLang("info-attribute-argument", "${key.group.name}:${key.name}", value, key.config.keys.joinToString(", "))
                }
            }
        }
    }

    @CommandBody
    val item = subCommand {
        exec<Player> {
            val item = sender.inventory.itemInMainHand
            val context = item.context() ?: return@exec
            sender.sendLang("info-item", context.key, context.hashcode)
            context.map().forEach {
                sender.sendLang("info-item-argument", it.key, it.value.toString(), it.value.javaClass.simpleName)
            }
        }
    }
}