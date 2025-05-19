package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.attribute.Crit.PhysicalChance.config
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.util.NODENS_NAMESPACE
import taboolib.common.util.random

object Crit: IAttributeGroup {

    override val name: String = "Crit"

    override val numbers: Map<String, IAttributeGroup.Number> = arrayOf(PhysicalChance, MagicChance, Addon, CritChanceResistance, CritAddonResistance).associateBy { it.name }

    object PhysicalChance: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = Crit

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            if (damageProcessor.damageType == Damage.Physics.name) {
                Crit.handleAttacker(damageProcessor, valueMap)
            }
        }
    }

    object MagicChance: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = Crit

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            if (damageProcessor.damageType == Damage.Magic.name) {
                Crit.handleAttacker(damageProcessor, valueMap)
            }
        }
    }

    private fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
        val chance = when (config.valueType) {
            IAttributeGroup.Number.ValueType.SINGLE -> valueMap[PERCENT]!![0]
            IAttributeGroup.Number.ValueType.RANGE -> random(valueMap[PERCENT]!![0], valueMap[PERCENT]!![1])
        }
        val resistance = damageProcessor.defender.attributeMemory()?.mergedAttribute(CritChanceResistance)
        val resistanceChance = when (CritChanceResistance.config.valueType) {
            IAttributeGroup.Number.ValueType.SINGLE -> resistance?.get(PERCENT)?.get(0) ?: 0.0
            IAttributeGroup.Number.ValueType.RANGE -> resistance?.get(PERCENT)?.let { random(it[0], it[1]) } ?: 0.0
        }
        if (random((chance - resistanceChance).coerceAtLeast(0.0).coerceAtMost(1.0))) {
            damageProcessor.crit = true
        }
    }

    object Addon: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = Crit

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            if (damageProcessor.crit) {
                val addon = when (MagicChance.config.valueType) {
                    IAttributeGroup.Number.ValueType.SINGLE -> valueMap[PERCENT]!![0]
                    IAttributeGroup.Number.ValueType.RANGE -> random(valueMap[PERCENT]!![0], valueMap[PERCENT]!![1])
                }
                damageProcessor.addDamageSource("$NODENS_NAMESPACE${Crit.name}$name", this, damageProcessor.getFinalDamage() * addon)
            }
        }
    }

    object CritChanceResistance: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = Crit
    }

    object CritAddonResistance: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = Crit

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            if (damageProcessor.crit) {
                val percent = when (config.valueType) {
                    IAttributeGroup.Number.ValueType.SINGLE -> valueMap[PERCENT]!![0]
                    IAttributeGroup.Number.ValueType.RANGE -> random(valueMap[PERCENT]!![0], valueMap[PERCENT]!![1])
                }
                val damage = damageProcessor.getDamageSource("$NODENS_NAMESPACE${Crit.name}${Addon.name}")?.damage ?: return
                damageProcessor.addDefenceSource("$NODENS_NAMESPACE${Crit.name}$name", this, damage * percent)
            }
        }
    }
}