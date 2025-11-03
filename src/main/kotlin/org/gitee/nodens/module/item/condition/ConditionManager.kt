package org.gitee.nodens.module.item.condition

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.common.FastMatchingMap
import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.util.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.io.runningClassesWithoutLibrary
import taboolib.common.platform.Awake

object ConditionManager {

    internal val CONDITION_MATCHING_MAP = FastMatchingMap<ICondition>()
    internal val conditionMap = mutableMapOf<String, ICondition>()

    const val SLOT_DATA_KEY = "slot"
    const val SLOT_IDENTIFY_KEY = "identify"

    @Reload(0)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        CONDITION_MATCHING_MAP.clear()
        conditionMap.clear()
        runningClassesWithoutLibrary.forEach {
            if (it.hasInterface(ICondition::class.java)) {
                val instance = it.getInstance() as ICondition
                conditionMap[instance::class.java.simpleName] = instance
                instance.keywords.forEach { key ->
                    CONDITION_MATCHING_MAP.put(key, instance)
                }
            }
        }
        consoleMessage(
            "&e┣&7ConditionMatchingMap loaded &a√",
            "&e┣&7Condition loaded &e${conditionMap.size} &a√"
        )
    }

    /**
     * 匹配条件是否通过
     *
     * @param livingEntity 被检测的实体
     * @param itemStack 被检测的物品
     * @param ignoreCondition 忽略的条件
     * @param map 额外检测参数
     * */
    fun matchConditions(livingEntity: LivingEntity, itemStack: ItemStack, ignoreCondition: Array<ICondition>, map: Map<String, String>): Boolean {
        val matchResults = mutableMapOf<String, String?>()
        itemStack.itemMeta?.lore?.forEach { line ->
            CONDITION_MATCHING_MAP.getMatchResult(line)?.let { matchResult ->
                matchResults[matchResult.value::class.java.simpleName] = matchResult.remain
            }
            // 如果没 lore 跳过
        } ?: return false
        return conditionMap.values.all { condition ->
            if (condition in ignoreCondition) return@all true
            condition.check(livingEntity, itemStack, matchResults[condition::class.java.simpleName], map)
        }
    }
}