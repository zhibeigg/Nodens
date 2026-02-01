package org.gitee.nodens.core.attribute

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.common.RegainProcessor.Companion.NATURAL_REASON
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.RANGE
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.SINGLE
import org.gitee.nodens.core.attribute.Crit.MagicChance
import org.gitee.nodens.util.NODENS_NAMESPACE
import org.gitee.nodens.util.addBukkitAttribute
import org.gitee.nodens.util.maxHealth
import taboolib.common.util.random
import taboolib.module.configuration.util.ReloadAwareLazy

object Health: IAttributeGroup {

    override val name: String = "Health"

    override val numbers: Map<String, IAttributeGroup.Number> = arrayOf(Max, Regain, RegainAddon, Heal, GrievousWounds, Healer).associateBy { it.name }

    object Max: AbstractSyncNumber() {

        override val group: IAttributeGroup
            get() = Health

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            addBukkitAttribute(Attribute.GENERIC_MAX_HEALTH, entitySyncProfile, valueMap)
        }
    }

    object Regain: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Health

        val period by ReloadAwareLazy(Nodens.config) { config.getLong("period", 20) }

        override fun handlePassive(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            if (regainProcessor.reason == NATURAL_REASON) {
                regainProcessor.addRegainSource("$NODENS_NAMESPACE${Health.name}$name", this, getRegain(valueMap))
            }
        }
    }

    private fun IAttributeGroup.Number.getRegain(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
        var regain = 0.0
        when(config.valueType) {
            SINGLE -> {
                valueMap.forEach { (type, double) ->
                    regain += when(type) {
                        PERCENT -> (valueMap[COUNT]?.get(0) ?: 0.0) * double[0]
                        COUNT -> double[0]
                    }
                }
            }
            RANGE -> {
                valueMap.forEach { (type, double) ->
                    regain += when(type) {
                        PERCENT -> (valueMap[COUNT]?.get(0) ?: 0.0) * random(double[0], double[1])
                        COUNT -> random(double[0], double[1])
                    }
                }
            }
        }
        return regain
    }

    object RegainAddon: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = Health

        override fun handlePassive(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            if (regainProcessor.reason == NATURAL_REASON) {
                regainProcessor.addRegainSource("$NODENS_NAMESPACE${Health.name}$name", this, getPercentRegain(regainProcessor.passive, valueMap))
            }
        }
    }

    private fun IAttributeGroup.Number.getPercentRegain(entity: LivingEntity, valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
        var regain = 0.0
        when(config.valueType) {
            SINGLE -> {
                valueMap.forEach { (type, double) ->
                    regain += when(type) {
                        PERCENT -> entity.maxHealth() * double[0]
                        COUNT -> 0.0
                    }
                }
            }
            RANGE -> {
                valueMap.forEach { (type, double) ->
                    regain += when(type) {
                        PERCENT -> entity.maxHealth() * random(double[0], double[1])
                        COUNT -> 0.0
                    }
                }
            }
        }
        return regain
    }

    object Heal: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Health

        override fun handleHealer(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            var heal = 0.0
            when(config.valueType) {
                SINGLE -> {
                    valueMap.forEach { (type, double) ->
                        heal += when(type) {
                            PERCENT -> (valueMap[COUNT]?.get(0) ?: 0.0) * double[0]
                            COUNT -> double[0]
                        }
                    }
                }
                RANGE -> {
                    valueMap.forEach { (type, double) ->
                        heal += when(type) {
                            PERCENT -> (valueMap[COUNT]?.get(0) ?: 0.0) * random(double[0], double[1])
                            COUNT -> random(double[0], double[1])
                        }
                    }
                }
            }
            regainProcessor.addRegainSource("$NODENS_NAMESPACE${Health.name}$name", this, heal)
        }
    }

    object GrievousWounds: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = Health

        override fun handlePassive(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            val percent = when (MagicChance.config.valueType) {
                SINGLE -> valueMap[PERCENT]!![0]
                RANGE -> random(valueMap[PERCENT]!![0], valueMap[PERCENT]!![1])
            }
            val value = regainProcessor.getFinalRegain()
            regainProcessor.addReduceSource("$NODENS_NAMESPACE${Health.name}$name", this, value * percent)
        }
    }

    object Healer: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = Health

        override fun handleHealer(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            val percent = when (MagicChance.config.valueType) {
                SINGLE -> valueMap[PERCENT]!![0]
                RANGE -> random(valueMap[PERCENT]!![0], valueMap[PERCENT]!![1])
            }
            val value = regainProcessor.getFinalRegain()
            regainProcessor.addRegainSource("$NODENS_NAMESPACE${Health.name}$name", this, value * percent)
        }
    }
}