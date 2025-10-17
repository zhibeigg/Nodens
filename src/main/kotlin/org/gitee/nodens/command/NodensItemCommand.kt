package org.gitee.nodens.command

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.nodens.api.events.item.NodensItemUpdateEvents
import org.gitee.nodens.module.item.ItemManager
import org.gitee.nodens.module.item.generator.NormalGenerator
import org.gitee.nodens.module.ui.ItemConfigManagerUI
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common5.cint
import taboolib.platform.util.giveItem
import taboolib.platform.util.isAir

@CommandHeader("item", description = "Nodens属性插件物品指令")
object NodensItemCommand {

    @CommandBody
    val manager = subCommand {
        exec<Player> {
            ItemConfigManagerUI(sender).open()
        }
    }

    @CommandBody
    val give = subCommand {
        player {
            dynamic("item") {
                suggest { ItemManager.itemConfigs.filter { !it.value.ignoreGenerate }.map { it.key } }
                int("amount") {
                    exec<ProxyCommandSender> {
                        val player = Bukkit.getPlayerExact(ctx["player"]) ?: return@exec
                        val item = ItemManager.getItemConfig(ctx["item"]) ?: return@exec
                        val amount = ctx["amount"].cint
                        player.giveItem(NormalGenerator.generate(item, amount, player))
                    }
                    dynamic("variable") {
                        exec<ProxyCommandSender> {
                            val player = Bukkit.getPlayerExact(ctx["player"]) ?: return@exec
                            val item = ItemManager.getItemConfig(ctx["item"]) ?: return@exec
                            val amount = ctx["amount"].cint
                            val variable = ctx["variable"].split(",").associate {
                                val split = it.split("=")
                                split[0] to split[1]
                            }
                            player.giveItem(NormalGenerator.generate(item, amount, player, variable))
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val update = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = Bukkit.getPlayerExact(ctx["player"]) ?: return@exec
                player.inventory.contents.forEachIndexed { index, item ->
                    if (item.isAir) return@forEachIndexed
                    val new = NormalGenerator.update(player, item)
                    player.inventory.setItem(index, new)
                    NodensItemUpdateEvents.Post(item, new).call()
                }
                player.updateInventory()
            }
        }
    }
}