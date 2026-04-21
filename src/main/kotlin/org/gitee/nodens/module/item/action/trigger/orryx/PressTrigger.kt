package org.gitee.nodens.module.item.action.trigger.orryx

import org.bukkit.entity.Player
import org.gitee.nodens.module.item.action.ActionContext
import org.gitee.nodens.module.item.action.ActionTrigger
import org.gitee.nodens.module.item.action.PluginDepend
import org.gitee.orryx.api.events.player.press.OrryxPlayerPressStartEvent
import org.gitee.orryx.api.events.player.press.OrryxPlayerPressStopEvent
import org.gitee.orryx.api.events.player.press.OrryxPlayerPressTickEvent

/**
 * Orryx 按键开始触发器
 * 玩家开始按下技能键时触发
 *
 * 注入变量:
 * - skill: ISkill 实例（可能为 null）
 * - pressTick: 按压时长
 */
@PluginDepend("Orryx")
object PressStartTrigger : ActionTrigger<OrryxPlayerPressStartEvent>() {

    override val id = "orryx_press_start"
    override val eventClass = OrryxPlayerPressStartEvent::class.java

    override fun extract(event: OrryxPlayerPressStartEvent): Player = event.player

    override fun populate(context: ActionContext, event: OrryxPlayerPressStartEvent) {
        context["skill"] = event.skill
        context["pressTick"] = event.pressTick
    }
}

/**
 * Orryx 按键停止触发器
 * 玩家松开技能键时触发
 *
 * 注入变量:
 * - skill: ISkill 实例（可能为 null）
 * - pressTick: 实际按压时长
 * - maxPressTick: 最大按压时长
 */
@PluginDepend("Orryx")
object PressStopTrigger : ActionTrigger<OrryxPlayerPressStopEvent>() {

    override val id = "orryx_press_stop"
    override val eventClass = OrryxPlayerPressStopEvent::class.java

    override fun extract(event: OrryxPlayerPressStopEvent): Player = event.player

    override fun populate(context: ActionContext, event: OrryxPlayerPressStopEvent) {
        context["skill"] = event.skill
        context["pressTick"] = event.pressTick
        context["maxPressTick"] = event.maxPressTick
    }
}

/**
 * Orryx 按键持续触发器
 * 玩家持续按住技能键时每 tick 触发
 *
 * 注入变量:
 * - skill: ISkill 实例（可能为 null）
 * - period: 周期
 * - pressTick: 当前按压时长
 * - maxPressTick: 最大按压时长
 */
@PluginDepend("Orryx")
object PressTickTrigger : ActionTrigger<OrryxPlayerPressTickEvent>() {

    override val id = "orryx_press_tick"
    override val eventClass = OrryxPlayerPressTickEvent::class.java

    override fun extract(event: OrryxPlayerPressTickEvent): Player = event.player

    override fun populate(context: ActionContext, event: OrryxPlayerPressTickEvent) {
        context["skill"] = event.skill
        context["period"] = event.period
        context["pressTick"] = event.pressTick
        context["maxPressTick"] = event.maxPressTick
    }
}
