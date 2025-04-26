package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DigitalParser

abstract class AbstractPercentNumber(): AbstractNumber() {

    override fun combatPower(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
        var value = 0.0
        val percent = valueMap[DigitalParser.Type.PERCENT]
        if (percent != null) {
            value = if (percent.size == 2) {
                (percent[0] + percent[1]) / 2 * 100
            } else {
                percent[0] * 100
            }
        }
        return value * config.combatPower
    }
}