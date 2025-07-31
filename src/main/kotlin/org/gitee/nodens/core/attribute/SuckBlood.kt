package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.RANGE
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.SINGLE
import org.gitee.nodens.util.maxHealth
import taboolib.common.util.random

object SuckBlood: IAttributeGroup {

    override val name: String = "SuckBlood"

    override val numbers: Map<String, IAttributeGroup.Number> = arrayOf(Value, Addon).associateBy { it.name }

    object Value: AbstractNumber() {

        override val group: IAttributeGroup
            get() = SuckBlood

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            val suckCount = when (config.valueType) {
                RANGE -> valueMap[COUNT]?.let { random(it[0], it[1]) }
                SINGLE -> valueMap[COUNT]?.get(0)
            } ?: 0.0
            damageProcessor.onDamage(config.handlePriority) {
                damageProcessor.attacker.health = (damageProcessor.attacker.health + suckCount).coerceAtMost(damageProcessor.attacker.maxHealth())
            }
        }
    }

    object Addon: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = SuckBlood

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            val suckPercent = when (config.valueType) {
                RANGE -> valueMap[PERCENT]?.let { random(it[0], it[1]) }
                SINGLE -> valueMap[PERCENT]?.get(0)
            } ?: 0.0
            damageProcessor.onDamage(config.handlePriority) {
                damageProcessor.attacker.health = (damageProcessor.attacker.health + it * suckPercent).coerceAtMost(damageProcessor.attacker.maxHealth())
            }
        }
    }
}