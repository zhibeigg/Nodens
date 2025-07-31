package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.*

abstract class AbstractNumber: IAttributeGroup.Number {

    override val name: String
        get() = this::class.java.simpleName

    override val config: AttributeConfig
        get() = AttributeManager.getConfig(group.name, name)

    override fun sync(
        entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    }

    override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    }

    override fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    }

    override fun handleHealer(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    }

    override fun handlePassive(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    }

    private fun getValue(valueMap: Map<DigitalParser.Type, DoubleArray>): Pair<Double, Double> {
        var value: Pair<Double, Double> = 0.0 to 0.0
        val count = valueMap[COUNT]
        if (count != null) {
            value = if (count.size == 2) {
                count[0] to count[1]
            } else {
                count[0] to count[0]
            }
        }
        val percent = valueMap[PERCENT]
        if (percent != null) {
            value = value.first * (1 + percent[0]) to value.second * (1 + percent[0])
        }
        return value
    }

    override fun combatPower(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
        return when(this@AbstractNumber.config.valueType) {
            RANGE -> (getValue(valueMap).first + getValue(valueMap).second)/2 * config.combatPower
            SINGLE -> getValue(valueMap).first * config.combatPower
        }
    }

    override fun getFinalValue(valueMap: Map<DigitalParser.Type, DoubleArray>): IAttributeGroup.Number.FinalValue {

        return object : IAttributeGroup.Number.FinalValue {

            override val type: IAttributeGroup.Number.ValueType
                get() = this@AbstractNumber.config.valueType

            override val value: Double? =  when(this@AbstractNumber.config.valueType) {
                RANGE -> null
                SINGLE -> getValue(valueMap).first
            }

            override val rangeValue: Pair<Double, Double>? =  when(this@AbstractNumber.config.valueType) {
                RANGE -> getValue(valueMap)
                SINGLE -> null
            }
        }
    }

    override fun toString(): String {
        return "AttributeNumber{name: $name}"
    }
}