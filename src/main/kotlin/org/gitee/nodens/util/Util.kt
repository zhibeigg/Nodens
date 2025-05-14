package org.gitee.nodens.util

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DigitalParser
import taboolib.module.configuration.Configuration
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ReloadableLazy<T>(private val check: () -> Any?, private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
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

class ConfigLazy<T>(val config: Configuration, private val initializer: Configuration.() -> T) : ReadOnlyProperty<Any?, T> {
    private var cached: T? = null
    private var initialized: Boolean = false
    private var lastHash: Int? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val currentHash = config.toString().hashCode()
        if (!initialized || lastHash != currentHash) {
            cached = initializer(config)
            initialized = true
            lastHash = currentHash
        }
        @Suppress("UNCHECKED_CAST")
        return cached as T
    }
}

fun LivingEntity.maxHealth(): Double {
    return getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: maxHealth
}