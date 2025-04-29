package org.gitee.nodens.api.events.item

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.module.item.ItemConfig
import org.gitee.nodens.module.item.NormalContext
import taboolib.platform.type.BukkitProxyEvent

class NodensItemGenerateEvent(val player: Player?, val itemConfig: ItemConfig, val context: NormalContext, val item: ItemStack) : BukkitProxyEvent()