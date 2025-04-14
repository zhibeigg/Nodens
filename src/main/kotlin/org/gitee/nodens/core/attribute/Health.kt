package org.gitee.nodens.core.attribute

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.*
import org.gitee.nodens.util.NODENS_NAMESPACE
import org.gitee.nodens.util.ReloadableLazy
import org.gitee.nodens.util.addBukkitAttribute
import taboolib.common.util.random

object Health: IAttributeGroup {

    override val name: String = "Health"

    object Max: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(Health.name, name)

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            addBukkitAttribute(Health, Attribute.GENERIC_MAX_HEALTH, entitySyncProfile, valueMap)
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
                            PERCENT -> (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: entity.maxHealth) * random(value[0], value[1])
                            COUNT -> random(value[0], value[1])
                        }
                    }

                    SINGLE -> {
                        when (key) {
                            PERCENT -> (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: entity.maxHealth) * value[0]
                            COUNT -> value[0]
                        }
                    }
                }
            }
            return regain
        }
    }
}