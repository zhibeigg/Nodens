package org.gitee.nodens.core.attribute

import org.apache.commons.lang3.tuple.Pair
import org.bukkit.entity.LivingEntity
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
        var value: Pair<Double, Double> = Pair.of(0.0, 0.0)
        val count = valueMap[COUNT]
        if (count != null) {
            value = if (count.size == 2) {
                Pair.of(count[0], count[1])
            } else {
                Pair.of(count[0], count[0])
            }
        }
        val percent = valueMap[PERCENT]
        if (percent != null) {
            value = Pair.of(value.left * (1 + percent[0]), value.right * (1 + percent[0]))
        }
        return value
    }

    override fun combatPower(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
        return when(this@AbstractNumber.config.valueType) {
            RANGE -> (getValue(valueMap).left + getValue(valueMap).right)/2 * config.combatPower
            SINGLE -> getValue(valueMap).left * config.combatPower
        }
    }

    override fun getFinalValue(entity: LivingEntity, valueMap: Map<DigitalParser.Type, DoubleArray>): IAttributeGroup.Number.FinalValue {

        return object : IAttributeGroup.Number.FinalValue {

            override val type: IAttributeGroup.Number.ValueType
                get() = this@AbstractNumber.config.valueType

            override val value: Double? =  when(this@AbstractNumber.config.valueType) {
                RANGE -> null
                SINGLE -> getValue(valueMap).left
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