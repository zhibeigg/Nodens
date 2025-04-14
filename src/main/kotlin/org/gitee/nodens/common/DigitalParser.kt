package org.gitee.nodens.common

import org.gitee.nodens.core.IAttributeGroup
import taboolib.common5.cdouble

class DigitalParser(private val value: String, private val number: IAttributeGroup.Number) {

    class Value(val type: Type, val doubleArray: DoubleArray)

    private lateinit var type: Type

    fun getDoubleArray(): Value {
        val list = value.split("-").map {
            if (it.last() == '%') {
                type = Type.PERCENT
                it.dropLast(1).cdouble / 100.0
            } else {
                type = Type.COUNT
                it.cdouble
            }
        }
        return when(number.config.valueType) {
            IAttributeGroup.Number.ValueType.RANGE -> {
                if (list.size > 1) {
                    Value(type, list.toDoubleArray())
                } else {
                    Value(type, doubleArrayOf(list[0], list[0]))
                }
            }
            IAttributeGroup.Number.ValueType.SINGLE -> Value(type, doubleArrayOf(list[0]))
        }
    }

    enum class Type {
        PERCENT, COUNT;
    }
}