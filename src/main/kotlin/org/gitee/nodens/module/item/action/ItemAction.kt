package org.gitee.nodens.module.item.action

/**
 * 物品动作数据
 * 从 YAML 配置的 actions 节解析而来
 *
 * @param triggers 触发器 ID 集合（逗号分隔解析后的结果）
 * @param scripts Kether 脚本行列表
 */
class ItemAction(
    val triggers: Set<String>,
    val scripts: List<String>
) {

    companion object {

        /**
         * 从配置 Map 解析 ItemAction
         * YAML 格式:
         * ```yaml
         * - trigger: "attack,attacked"
         *   action:
         *     - "kether script"
         * ```
         */
        fun parse(map: Map<*, *>): ItemAction? {
            val triggerRaw = map["trigger"]?.toString() ?: return null
            val triggers = triggerRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
            if (triggers.isEmpty()) return null
            val scripts: List<String> = when (val action = map["action"]) {
                is List<*> -> action.filterIsInstance<String>()
                is String -> listOf(action)
                else -> return null
            }
            if (scripts.isEmpty()) return null
            return ItemAction(triggers, scripts)
        }
    }
}
