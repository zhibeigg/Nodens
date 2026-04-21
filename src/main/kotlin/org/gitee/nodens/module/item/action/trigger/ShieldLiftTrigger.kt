package org.gitee.nodens.module.item.action.trigger

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.gitee.nodens.module.item.action.ActionContext
import org.gitee.nodens.module.item.action.ActionTrigger
import taboolib.platform.util.isOffhand
import taboolib.platform.util.isRightClick

/**
 * 盾牌举起触发器
 * 当玩家主手持盾并右键时触发
 *
 * 注入变量: 无额外变量
 */
object ShieldLiftTrigger : ActionTrigger<PlayerInteractEvent>() {

    override val id = "shield_lift"
    override val eventClass = PlayerInteractEvent::class.java

    override fun extract(event: PlayerInteractEvent): Player? {
        val player = event.player
        if (!player.inventory.itemInMainHand.type.name.contains("SHIELD")) return null
        if (!event.isRightClick()) return null
        if (!event.isOffhand()) return null
        return player
    }
}
