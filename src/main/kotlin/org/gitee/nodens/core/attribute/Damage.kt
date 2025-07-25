package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.NODENS_NAMESPACE
import taboolib.common.util.random

object Damage: IAttributeGroup {

    override val name: String = "Damage"

    override val numbers: Map<String, IAttributeGroup.Number> = arrayOf(Physics, Magic, Real, Fire, Monster, Boss, Enhancement).associateBy { it.name }

    object Physics: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Damage

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.addDamageSource("$NODENS_NAMESPACE${Damage.name}$name", this, getDamage(valueMap))
        }
    }

    object Magic: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Damage

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.addDamageSource("$NODENS_NAMESPACE${Damage.name}$name", this, getDamage(valueMap))
        }
    }

    object Real: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Damage

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.addDamageSource("$NODENS_NAMESPACE${Damage.name}$name", this, getDamage(valueMap))
        }
    }

    object Fire: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Damage

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.addDamageSource("$NODENS_NAMESPACE${Damage.name}$name", this, getDamage(valueMap))
        }
    }

    object Monster: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Damage

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.addDamageSource("$NODENS_NAMESPACE${Damage.name}$name", this, getDamage(valueMap))
        }
    }

    object Boss: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Damage

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.addDamageSource("$NODENS_NAMESPACE${Damage.name}$name", this, getDamage(valueMap))
        }
    }

    object Enhancement: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Damage

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.addDamageSource("$NODENS_NAMESPACE${Damage.name}$name", this, getDamage(valueMap))
        }
    }

    private fun IAttributeGroup.Number.getDamage(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
        var damage = 0.0
        when(config.valueType) {
            IAttributeGroup.Number.ValueType.SINGLE -> {
                valueMap.forEach { (type, double) ->
                    damage += when(type) {
                        PERCENT -> (valueMap[COUNT]?.get(0) ?: 0.0) * double[0]
                        COUNT -> double[0]
                    }
                }
            }
            IAttributeGroup.Number.ValueType.RANGE -> {
                valueMap.forEach { (type, double) ->
                    damage += when(type) {
                        PERCENT -> (valueMap[COUNT]?.get(0) ?: 0.0) * random(double[0], double[1])
                        COUNT -> random(double[0], double[1])
                    }
                }
            }
        }
        return damage
    }
}