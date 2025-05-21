package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.NODENS_NAMESPACE
import taboolib.common.util.random

object Defence: IAttributeGroup {

    override val name: String = "Defence"

    override val numbers: Map<String, IAttributeGroup.Number> = arrayOf(Physics, Magic, Fire).associateBy { it.name }

    object Physics: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Defence

        override fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.addDefenceSource("$NODENS_NAMESPACE${Defence.name}$name", this, getDefence(valueMap))
        }
    }

    object Magic: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Defence

        override fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.addDefenceSource("$NODENS_NAMESPACE${Defence.name}$name", this, getDefence(valueMap))
        }
    }

    object Fire: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Defence

        override fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.addDefenceSource("$NODENS_NAMESPACE${Defence.name}$name", this, getDefence(valueMap))
        }
    }

    fun IAttributeGroup.Number.getDefence(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
        var defence = 0.0
        when(config.valueType) {
            IAttributeGroup.Number.ValueType.SINGLE -> {
                valueMap.forEach { (type, double) ->
                    defence += when(type) {
                        PERCENT -> (valueMap[COUNT]?.get(0) ?: 0.0) * double[0]
                        COUNT -> double[0]
                    }
                }
            }
            IAttributeGroup.Number.ValueType.RANGE -> {
                valueMap.forEach { (type, double) ->
                    defence += when(type) {
                        PERCENT -> (valueMap[COUNT]?.get(0) ?: 0.0) * random(double[0], double[1])
                        COUNT -> random(double[0], double[1])
                    }
                }
            }
        }
        return defence
    }
}