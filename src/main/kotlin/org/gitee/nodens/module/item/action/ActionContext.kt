package org.gitee.nodens.module.item.action

import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * 物品动作执行上下文
 * 承载事件触发时的所有相关数据，传递给 Kether 脚本
 */
class ActionContext(
    val player: Player,
    val event: Event,
    val triggerId: String
) {

    /** 自定义变量表，由触发器 populate 填充 */
    val variables: MutableMap<String, Any?> = HashMap(8)

    operator fun set(key: String, value: Any?) {
        variables[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? = variables[key] as? T
}
