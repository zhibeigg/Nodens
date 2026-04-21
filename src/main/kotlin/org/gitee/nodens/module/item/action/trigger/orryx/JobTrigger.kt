package org.gitee.nodens.module.item.action.trigger.orryx

import org.bukkit.entity.Player
import org.gitee.nodens.module.item.action.ActionContext
import org.gitee.nodens.module.item.action.ActionTrigger
import org.gitee.nodens.module.item.action.PluginDepend
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents

/**
 * Orryx 职业变更触发器
 * 玩家职业变更后触发
 *
 * 注入变量:
 * - job: IPlayerJob 实例
 */
@PluginDepend("Orryx")
object JobChangeTrigger : ActionTrigger<OrryxPlayerJobChangeEvents.Post>() {

    override val id = "orryx_job_change"
    override val eventClass = OrryxPlayerJobChangeEvents.Post::class.java

    override fun extract(event: OrryxPlayerJobChangeEvents.Post): Player = event.player

    override fun populate(context: ActionContext, event: OrryxPlayerJobChangeEvents.Post) {
        context["job"] = event.job
    }
}
