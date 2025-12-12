package org.gitee.nodens.module.item

import kotlinx.serialization.Serializable

interface Variable<T> {

    val value: T?
}

@Serializable
data class NullVariable(override val value: Nothing?): Variable<Nothing?>

@Serializable
data class ByteVariable(override val value: Byte): Variable<Byte>

@Serializable
data class ShortVariable(override val value: Short): Variable<Short>

@Serializable
data class IntVariable(override val value: Int): Variable<Int>

@Serializable
data class LongVariable(override val value: Long): Variable<Long>

@Serializable
data class FloatVariable(override val value: Float): Variable<Float>

@Serializable
data class DoubleVariable(override val value: Double): Variable<Double>

@Serializable
data class CharVariable(override val value: Char): Variable<Char>

@Serializable
data class StringVariable(override val value: String): Variable<String>

@Serializable
data class BooleanVariable(override val value: Boolean): Variable<Boolean>

@Serializable
data class ArrayVariable(override val value: List<@Serializable Variable<*>>): Variable<List<Variable<*>>>

@Serializable
data class MapVariable(override val value: Map<String, @Serializable Variable<*>>): Variable<Map<String, Variable<*>>>