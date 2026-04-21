package org.gitee.nodens.module.item.action.trigger.orryx

import org.bukkit.entity.Player
import org.gitee.nodens.module.item.action.ActionContext
import org.gitee.nodens.module.item.action.ActionTrigger
import org.gitee.nodens.module.item.action.PluginDepend
import org.gitee.orryx.api.events.damage.OrryxDamageEvents

/**
 * Orryx 伤害前触发器（攻击者视角）
 * 伤害计算前触发，可修改伤害值
 *
 * 注入变量:
 * - attacker: 攻击者实体
 * - defender: 防御者实体
 * - damage: 伤害值
 * - damageType: 伤害类型
 */
@PluginDepend("Orryx")
object DamagePreAttackerTrigger : ActionTrigger<OrryxDamageEvents.Pre>() {

    override val id = "orryx_damage_pre_attacker"
    override val eventClass = OrryxDamageEvents.Pre::class.java

    override fun extract(event: OrryxDamageEvents.Pre): Player? = event.attackPlayer()

    override fun populate(context: ActionContext, event: OrryxDamageEvents.Pre) {
        context["attacker"] = event.attacker
        context["defender"] = event.defender
        context["damage"] = event.damage
        context["damageType"] = event.type.name
    }
}

/**
 * Orryx 伤害前触发器（防御者视角）
 * 伤害计算前触发，可修改伤害值
 *
 * 注入变量:
 * - attacker: 攻击者实体
 * - defender: 防御者实体
 * - damage: 伤害值
 * - damageType: 伤害类型
 */
@PluginDepend("Orryx")
object DamagePreDefenderTrigger : ActionTrigger<OrryxDamageEvents.Pre>() {

    override val id = "orryx_damage_pre_defender"
    override val eventClass = OrryxDamageEvents.Pre::class.java

    override fun extract(event: OrryxDamageEvents.Pre): Player? = event.defenderPlayer()

    override fun populate(context: ActionContext, event: OrryxDamageEvents.Pre) {
        context["attacker"] = event.attacker
        context["defender"] = event.defender
        context["damage"] = event.damage
        context["damageType"] = event.type.name
    }
}

/**
 * Orryx 伤害后触发器（攻击者视角）
 * 伤害计算完成后触发
 *
 * 注入变量:
 * - attacker: 攻击者实体
 * - defender: 防御者实体
 * - damage: 最终伤害值
 * - damageType: 伤害类型
 * - crit: 是否暴击
 */
@PluginDepend("Orryx")
object DamagePostAttackerTrigger : ActionTrigger<OrryxDamageEvents.Post>() {

    override val id = "orryx_damage_post_attacker"
    override val eventClass = OrryxDamageEvents.Post::class.java

    override fun extract(event: OrryxDamageEvents.Post): Player? = event.attackPlayer()

    override fun populate(context: ActionContext, event: OrryxDamageEvents.Post) {
        context["attacker"] = event.attacker
        context["defender"] = event.defender
        context["damage"] = event.damage
        context["damageType"] = event.type.name
        context["crit"] = event.crit
    }
}

/**
 * Orryx 伤害后触发器（防御者视角）
 * 伤害计算完成后触发
 *
 * 注入变量:
 * - attacker: 攻击者实体
 * - defender: 防御者实体
 * - damage: 最终伤害值
 * - damageType: 伤害类型
 * - crit: 是否暴击
 */
@PluginDepend("Orryx")
object DamagePostDefenderTrigger : ActionTrigger<OrryxDamageEvents.Post>() {

    override val id = "orryx_damage_post_defender"
    override val eventClass = OrryxDamageEvents.Post::class.java

    override fun extract(event: OrryxDamageEvents.Post): Player? = event.defenderPlayer()

    override fun populate(context: ActionContext, event: OrryxDamageEvents.Post) {
        context["attacker"] = event.attacker
        context["defender"] = event.defender
        context["damage"] = event.damage
        context["damageType"] = event.type.name
        context["crit"] = event.crit
    }
}
