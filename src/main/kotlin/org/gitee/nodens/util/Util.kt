package org.gitee.nodens.util

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.reload.ReloadAPI
import java.util.EnumMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 合并多个 [DigitalParser.Value] 值，按类型分组并累加对应位置的数值。
 *
 * 采用单次遍历 + EnumMap 的高性能算法：
 * - 时间复杂度：O(n * m)，其中 n 为 values 数量，m 为平均数组长度
 * - 空间复杂度：O(k * m)，其中 k 为类型数量（最多2个）
 * - 避免创建中间分组对象，减少 GC 压力
 *
 * @param values 要合并的值数组
 * @return 按类型分组的合并结果，每个类型对应一个累加后的 DoubleArray
 */
fun mergeValues(vararg values: DigitalParser.Value): Map<DigitalParser.Type, DoubleArray> {
    if (values.isEmpty()) return emptyMap()

    val result = EnumMap<DigitalParser.Type, DoubleArray>(DigitalParser.Type::class.java)

    for (value in values) {
        val type = value.type
        val arr = value.doubleArray
        val existing = result[type]

        if (existing == null) {
            result[type] = arr.copyOf()
        } else if (arr.size <= existing.size) {
            for (i in arr.indices) {
                existing[i] += arr[i]
            }
        } else {
            val newArr = arr.copyOf()
            for (i in existing.indices) {
                newArr[i] += existing[i]
            }
            result[type] = newArr
        }
    }

    return result
}

fun LivingEntity.maxHealth(): Double {
    return getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: maxHealth
}

fun comparePriority(o1: Int, o2: Int): Int {
    return when {
        o1 > o2 -> 1
        o1 < o2 -> -1
        else -> 0
    }
}