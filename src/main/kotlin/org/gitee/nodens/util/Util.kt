package org.gitee.nodens.util

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.reload.ReloadAPI
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun mergeValues(vararg values: DigitalParser.Value): Map<DigitalParser.Type, DoubleArray> {
    val group = values.groupBy { it.type }
    val map = hashMapOf<DigitalParser.Type, DoubleArray>()
    DigitalParser.Type.entries.forEach { type ->
        val list = group[type] ?: return@forEach
        val maxSize = list.maxOfOrNull { it.doubleArray.size } ?: 0
        val result = DoubleArray(maxSize)
        for (value in list) {
            for (i in value.doubleArray.indices) {
                result[i] += value.doubleArray[i]
            }
        }
        map[type] = result
    }
    return map
}

class MonitorLazy<T>(private val check: () -> Any?, private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private var cached: T? = null
    private var initialized: Boolean = false
    private var lastHash: Int? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val current = check()
        val currentHash = current.hashCode()
        if (!initialized || lastHash != currentHash) {
            cached = initializer()
            initialized = true
            lastHash = currentHash
        }
        @Suppress("UNCHECKED_CAST")
        return cached as T
    }
}

class ConfigLazy<T>(private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private var cached: T? = null
    private var initialized: Boolean = false
    private var lastMark: Short? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val currentMark = ReloadAPI.reloadMark
        if (!initialized || lastMark != currentMark) {
            cached = initializer()
            initialized = true
            lastMark = currentMark
        }
        @Suppress("UNCHECKED_CAST")
        return cached as T
    }
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