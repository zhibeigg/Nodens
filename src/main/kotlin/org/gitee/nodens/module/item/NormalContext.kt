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
        return variable[key]?.value
    }

    fun map(): Map<String, Any> {
        return variable.toMap().filter { it.value.value != null }.mapValues { it.value.value!! }
    }
}