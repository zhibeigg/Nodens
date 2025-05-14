package org.gitee.nodens.core.attribute

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.common.RegainProcessor.Companion.NATURAL_REASON
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.RANGE
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.SINGLE
import org.gitee.nodens.core.attribute.Crit.MagicChance
import org.gitee.nodens.util.NODENS_NAMESPACE
import org.gitee.nodens.util.ReloadableLazy
import org.gitee.nodens.util.addBukkitAttribute
import org.gitee.nodens.util.maxHealth
import taboolib.common.util.random

object Health: IAttributeGroup {

    override val name: String = "Health"

    object Max: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(Health.name, name)

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            addBukkitAttribute(Attribute.GENERIC_MAX_HEALTH, entitySyncProfile, valueMap)
        }
    }

    object Regain: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(Health.name, name)

        val period by ReloadableLazy({ config }) { config.getLong("period", 20) }

        fun getRegain(entity: LivingEntity, map: Map<DigitalParser.Type, DoubleArray>): Double {
            var regain = 0.0
            map.forEach { (key, value) ->
                regain += when(config.valueType) {
                    RANGE -> {
                        when (key) {
                            PERCENT -> entity.maxHealth() * random(value[0], value[1])
                            COUNT -> random(value[0], value[1])
                        }
                    }

                    SINGLE -> {
                        when (key) {
                            PERCENT -> entity.maxHealth() * value[0]
                            COUNT -> value[0]
                        }
                    }
                }
            }
            return regain
        }

        override fun handlePassive(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            if (regainProcessor.reason == NATURAL_REASON) {
                regainProcessor.addRegainSource("$NODENS_NAMESPACE${Health.name}$name", this, getRegain(regainProcessor.passive, valueMap))
            }
        }
    }

    object GrievousWounds: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(Health.name, name)

        override fun handlePassive(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            val percent = when (MagicChance.config.valueType) {
                SINGLE -> valueMap[PERCENT]!![0]
                RANGE -> random(valueMap[PERCENT]!![0], valueMap[PERCENT]!![1])
            }
            val damage = regainProcessor.getFinalRegain()
            regainProcessor.addReduceSource("$NODENS_NAMESPACE${Health.name}$name", this, damage * percent)
        }
    }
}