package org.gitee.nodens.core

import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.*
import org.gitee.nodens.core.attribute.AbstractNumber
import java.math.BigDecimal

interface IAttributeGroup {

    // 属性组名
    val name: String

    val numbers : Map<String, Number>

    interface Number {

        val group: IAttributeGroup

        val name: String

        val config: AttributeConfig

        fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>)

        fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>)

        fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>)

        fun handleHealer(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>)

        fun handlePassive(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>)

        fun combatPower(valueMap: Map<DigitalParser.Type, DoubleArray>): Double

        fun getFinalValue(entity: LivingEntity, valueMap: Map<DigitalParser.Type, DoubleArray>): FinalValue

        interface FinalValue {

            val type: ValueType

            val value: Double?

            val rangeValue: Pair<Double, Double>?
        }

        enum class ValueType {
            RANGE, SINGLE;
        }
    }
}