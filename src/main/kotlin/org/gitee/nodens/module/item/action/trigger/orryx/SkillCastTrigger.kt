package org.gitee.nodens.module.item.action.trigger.orryx

import org.bukkit.entity.Player
import org.gitee.nodens.module.item.action.ActionContext
import org.gitee.nodens.module.item.action.ActionTrigger
import org.gitee.nodens.module.item.action.PluginDepend
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCastEvents

/**
 * Orryx 技能释放检查触发器
 * 技能释放前的检查阶段触发（可取消）
 *
 * 注入变量:
 * - skill: IPlayerSkill 实例
 * - skillParameter: IParameter 实例
 */
@PluginDepend("Orryx")
object SkillCastCheckTrigger : ActionTrigger<OrryxPlayerSkillCastEvents.Check>() {

    override val id = "orryx_skill_cast_check"
    override val eventClass = OrryxPlayerSkillCastEvents.Check::class.java

    override fun extract(event: OrryxPlayerSkillCastEvents.Check): Player = event.player

    override fun populate(context: ActionContext, event: OrryxPlayerSkillCastEvents.Check) {
        context["skill"] = event.skill
        context["skillParameter"] = event.skillParameter
    }
}

/**
 * Orryx 技能释放触发器
 * 技能实际释放时触发
 *
 * 注入变量:
 * - skill: IPlayerSkill 实例
 * - skillParameter: IParameter 实例
 */
@PluginDepend("Orryx")
object SkillCastTrigger : ActionTrigger<OrryxPlayerSkillCastEvents.Cast>() {

    override val id = "orryx_skill_cast"
    override val eventClass = OrryxPlayerSkillCastEvents.Cast::class.java

    override fun extract(event: OrryxPlayerSkillCastEvents.Cast): Player = event.player

    override fun populate(context: ActionContext, event: OrryxPlayerSkillCastEvents.Cast) {
        context["skill"] = event.skill
        context["skillParameter"] = event.skillParameter
    }
}
