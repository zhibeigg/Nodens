package org.gitee.nodens.module.item.action.trigger

import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.gitee.nodens.module.item.action.ActionContext
import org.gitee.nodens.module.item.action.ActionTrigger
import org.bukkit.inventory.EquipmentSlot

/**
 * 交互触发器（合并所有交互类型）
 *
 * 根据交互类型注册为不同的触发器 ID:
 * - left_click_air: 左键空气
 * - left_click_block: 左键方块
 * - right_click_air: 右键空气
 * - right_click_block: 右键方块
 *
 * 注入变量:
 * - block: 交互的方块（仅方块交互时）
 * - action: 交互类型字符串
 */
object LeftClickAirTrigger : ActionTrigger<PlayerInteractEvent>() {
    override val id = "left_click_air"
    override val eventClass = PlayerInteractEvent::class.java

    override fun extract(event: PlayerInteractEvent): Player? {
        if (event.hand != EquipmentSlot.HAND) return null
        if (event.action != Action.LEFT_CLICK_AIR) return null
        return event.player
    }

    override fun populate(context: ActionContext, event: PlayerInteractEvent) {
        context["action"] = event.action.name
    }
}

object LeftClickBlockTrigger : ActionTrigger<PlayerInteractEvent>() {
    override val id = "left_click_block"
    override val eventClass = PlayerInteractEvent::class.java

    override fun extract(event: PlayerInteractEvent): Player? {
        if (event.hand != EquipmentSlot.HAND) return null
        if (event.action != Action.LEFT_CLICK_BLOCK) return null
        return event.player
    }

    override fun populate(context: ActionContext, event: PlayerInteractEvent) {
        context["action"] = event.action.name
        context["block"] = event.clickedBlock
    }
}

object RightClickAirTrigger : ActionTrigger<PlayerInteractEvent>() {
    override val id = "right_click_air"
    override val eventClass = PlayerInteractEvent::class.java

    override fun extract(event: PlayerInteractEvent): Player? {
        if (event.hand != EquipmentSlot.HAND) return null
        if (event.action != Action.RIGHT_CLICK_AIR) return null
        return event.player
    }

    override fun populate(context: ActionContext, event: PlayerInteractEvent) {
        context["action"] = event.action.name
    }
}

object RightClickBlockTrigger : ActionTrigger<PlayerInteractEvent>() {
    override val id = "right_click_block"
    override val eventClass = PlayerInteractEvent::class.java

    override fun extract(event: PlayerInteractEvent): Player? {
        if (event.hand != EquipmentSlot.HAND) return null
        if (event.action != Action.RIGHT_CLICK_BLOCK) return null
        return event.player
    }

    override fun populate(context: ActionContext, event: PlayerInteractEvent) {
        context["action"] = event.action.name
        context["block"] = event.clickedBlock
    }
}
