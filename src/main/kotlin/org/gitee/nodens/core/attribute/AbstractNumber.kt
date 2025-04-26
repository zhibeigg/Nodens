package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.core.IAttributeGroup

abstract class AbstractNumber: IAttributeGroup.Number {

    override val name: String
        get() = this::class.java.simpleName

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

    override fun combatPower(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
        var value = 0.0
        val count = valueMap[COUNT]
        if (count != null) {
            value = if (count.size == 2) {
                (count[0] + count[1])/2
            } else {
                count[0]
            }
        }
        val percent = valueMap[PERCENT]
        if (percent != null) {
            value *= 1 + percent[0]
        }
        return value * config.combatPower
    }
}