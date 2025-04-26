package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.util.NODENS_NAMESPACE
import taboolib.common.util.random

object Defence: IAttributeGroup {

    override val name: String = "Defence"

    object Physics: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(Defence.name, name)

        override fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            if (damageProcessor.damageType == name) {
                damageProcessor.addDefenceSource("$NODENS_NAMESPACE${Defence.name}$name", this, getDefence(valueMap))
            }
        }
    }

    object Magic: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(Defence.name, name)

        override fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            if (damageProcessor.damageType == name) {
                damageProcessor.addDefenceSource("$NODENS_NAMESPACE${Defence.name}$name", this, getDefence(valueMap))
            }
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