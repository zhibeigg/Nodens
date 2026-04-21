package org.gitee.nodens.module.item.action.trigger

import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.module.item.action.ActionContext
import org.gitee.nodens.module.item.action.ActionTrigger
import taboolib.platform.util.attacker

/**
 * 被攻击触发器
 * 当玩家作为被攻击者受到伤害时触发
 *
 * 注入变量:
 * - attacker: 实际攻击者
 * - damager: 直接伤害来源实体
 * - entity: 被攻击的实体（即玩家自身）
 */
object AttackedTrigger : ActionTrigger<EntityDamageByEntityEvent>() {

    override val id = "attacked"
    override val eventClass = EntityDamageByEntityEvent::class.java

    override fun extract(event: EntityDamageByEntityEvent): Player? {
        return event.entity as? Player
    }

    override fun populate(context: ActionContext, event: EntityDamageByEntityEvent) {
        context["attacker"] = event.attacker
        context["damager"] = event.damager
        context["entity"] = event.entity
    }
}
