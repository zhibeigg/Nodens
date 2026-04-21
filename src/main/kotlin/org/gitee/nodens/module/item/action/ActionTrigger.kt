package org.gitee.nodens.module.item.action

import org.bukkit.entity.Player
import org.bukkit.event.Event
import taboolib.common.platform.event.EventPriority

/**
 * 物品动作触发器抽象基类
 *
 * 每个触发器绑定一种 Bukkit 事件，当事件触发时：
 * 1. [extract] 从事件中提取玩家，返回 null 则跳过
 * 2. [populate] 向 [ActionContext] 注入事件相关变量
 *
 * 实现方式：继承此类并声明为 object 即可被 [ActionTriggerManager] 自动发现注册
 *
 * @param E 监听的 Bukkit 事件类型
 */
abstract class ActionTrigger<E : Event> {

    /** 触发器唯一标识，用于 YAML 配置中的 trigger 字段匹配 */
    abstract val id: String

    /** 监听的事件类 */
    abstract val eventClass: Class<E>

    /** 事件监听优先级 */
    open val priority: EventPriority get() = EventPriority.HIGHEST

    /** 是否忽略已取消的事件 */
    open val ignoreCancelled: Boolean get() = false

    /**
     * 从事件中提取触发的玩家
     * @return 玩家实例，返回 null 表示此事件不满足触发条件
     */
    abstract fun extract(event: E): Player?

    /**
     * 向执行上下文注入事件相关变量
     * 子类可覆写此方法添加自定义变量（如 attacker, entity, block 等）
     */
    open fun populate(context: ActionContext, event: E) {}
}
