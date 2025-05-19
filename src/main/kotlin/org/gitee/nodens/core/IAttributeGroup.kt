package org.gitee.nodens.core

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.common.RegainProcessor
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

        enum class ValueType {
            RANGE, SINGLE;
        }
    }
}