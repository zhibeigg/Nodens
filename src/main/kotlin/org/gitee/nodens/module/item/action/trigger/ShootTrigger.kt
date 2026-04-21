package org.gitee.nodens.module.item.action.trigger

import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityShootBowEvent
import org.gitee.nodens.module.item.action.ActionContext
import org.gitee.nodens.module.item.action.ActionTrigger

/**
 * 射击触发器
 * 当玩家使用弓/弩射出弹射物时触发
 *
 * 注入变量:
 * - bow: 使用的弓
 * - consumable: 消耗的弹药
 * - projectile: 射出的弹射物实体
 */
object ShootTrigger : ActionTrigger<EntityShootBowEvent>() {

    override val id = "shoot"
    override val eventClass = EntityShootBowEvent::class.java

    override fun extract(event: EntityShootBowEvent): Player? {
        return event.entity as? Player
    }

    override fun populate(context: ActionContext, event: EntityShootBowEvent) {
        context["bow"] = event.bow
        context["consumable"] = event.consumable
        context["projectile"] = event.projectile
    }
}
