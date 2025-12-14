package org.gitee.nodens.module.item

import kotlinx.serialization.Serializable
import org.gitee.nodens.util.toVariable

@Serializable
data class NormalContext(override val key: String, private val variable: HashMap<String, Variable<*>>, override val hashcode: Int): IItemContext {

    operator fun set(key: String, value: Any?) {
        variable[key] = value.toVariable()
    }

    fun putAll(map: Map<String, Any?>) {
        variable.putAll(map.mapValues { it.value.toVariable() })
    }

    operator fun get(key: String): Any? {
        return restore(variable[key]?.value)
    }

    fun remove(key: String): Any? {
        return restore(variable.remove(key))
    }

    private fun restore(value: Any?): Any? {
        return when (value) {
            is List<*> -> value.map { restore((it as Variable<*>).value) }
            is Map<*, *> -> value.mapValues { restore((it.value as Variable<*>).value) }
            else -> value
        }
    }

    fun map(): Map<String, Any> {
        return variable.toMap().mapValues { restore(it.value.value)!! }
    }

    fun sourceMap(): Map<String, Variable<*>> {
        return variable.toMap()
    }
}