package org.gitee.nodens.module.item.action

/**
 * 标记触发器依赖的外部插件
 * 在 [ActionTriggerManager] 扫描时，若指定插件未加载则跳过注册
 *
 * @param name 依赖的插件名称
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PluginDepend(val name: String)
