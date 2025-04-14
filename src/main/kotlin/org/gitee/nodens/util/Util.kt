package org.gitee.nodens.util

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