package org.gitee.nodens.module.item.action.trigger.orryx

import org.bukkit.entity.Player
import org.gitee.nodens.module.item.action.ActionContext
import org.gitee.nodens.module.item.action.ActionTrigger
import org.gitee.nodens.module.item.action.PluginDepend
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillLevelEvents

/**
 * Orryx 技能升级触发器
 * 技能等级提升后触发
 *
 * 注入变量:
 * - skill: IPlayerSkill 实例
 * - upLevel: 提升的等级数
 */
@PluginDepend("Orryx")
object SkillLevelUpTrigger : ActionTrigger<OrryxPlayerSkillLevelEvents.Up.Post>() {

    override val id = "orryx_skill_level_up"
    override val eventClass = OrryxPlayerSkillLevelEvents.Up.Post::class.java

    override fun extract(event: OrryxPlayerSkillLevelEvents.Up.Post): Player = event.player

    override fun populate(context: ActionContext, event: OrryxPlayerSkillLevelEvents.Up.Post) {
        context["skill"] = event.skill
        context["upLevel"] = event.upLevel
    }
}

/**
 * Orryx 技能降级触发器
 * 技能等级降低后触发
 *
 * 注入变量:
 * - skill: IPlayerSkill 实例
 * - downLevel: 降低的等级数
 */
@PluginDepend("Orryx")
object SkillLevelDownTrigger : ActionTrigger<OrryxPlayerSkillLevelEvents.Down.Post>() {

    override val id = "orryx_skill_level_down"
    override val eventClass = OrryxPlayerSkillLevelEvents.Down.Post::class.java

    override fun extract(event: OrryxPlayerSkillLevelEvents.Down.Post): Player = event.player

    override fun populate(context: ActionContext, event: OrryxPlayerSkillLevelEvents.Down.Post) {
        context["skill"] = event.skill
        context["downLevel"] = event.downLevel
    }
}
