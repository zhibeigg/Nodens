package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.IAttributeGroup

abstract class AbstractPercentNumber(): AbstractNumber() {

    private fun getValue(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
        var value = 0.0
        val percent = valueMap[DigitalParser.Type.PERCENT]
        if (percent != null) {
            value = if (percent.size == 2) {
                (percent[0] + percent[1]) / 2 * 100
            } else {
                percent[0] * 100
            }
        }
        return value
    }

    override fun combatPower(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
        return getValue(valueMap) * config.combatPower
    }

    override fun getFinalValue(valueMap: Map<DigitalParser.Type, DoubleArray>): IAttributeGroup.Number.FinalValue {

        return object : IAttributeGroup.Number.FinalValue {

            override val type: IAttributeGroup.Number.ValueType
                get() = this@AbstractPercentNumber.config.valueType

            override val value: Double = getValue(valueMap)

            override val rangeValue: Pair<Double, Double>? = null
        }
    }
}