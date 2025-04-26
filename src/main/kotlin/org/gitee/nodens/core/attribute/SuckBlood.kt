package org.gitee.nodens.core.attribute

import org.bukkit.attribute.Attribute
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.*
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.*
import taboolib.common.util.random

object SuckBlood: IAttributeGroup {

    override val name: String = "SuckBlood"

    object Addon: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(SuckBlood.name, name)

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            val suckCount = when (config.valueType) {
                RANGE -> valueMap[COUNT]?.let { random(it[0], it[1]) }
                SINGLE -> valueMap[COUNT]?.get(0)
            } ?: 0.0
            val suckPercent = when (config.valueType) {
                RANGE -> valueMap[PERCENT]?.let { random(it[0], it[1]) }
                SINGLE -> valueMap[PERCENT]?.get(0)
            } ?: 0.0
            damageProcessor.onDamage(config.handlePriority) {
                damageProcessor.attacker.health = (damageProcessor.attacker.health + suckCount + it * suckPercent).coerceAtMost(damageProcessor.attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: damageProcessor.attacker.maxHealth)
            }
        }
    }
}