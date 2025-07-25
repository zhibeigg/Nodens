package org.gitee.nodens.core.attribute

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.RANGE
import org.gitee.nodens.core.IAttributeGroup.Number.ValueType.SINGLE
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.events.player.OrryxPlayerManaEvents
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.random

object Mana: IAttributeGroup {

    override val name: String = "Mana"

    override val numbers: Map<String, IAttributeGroup.Number> = arrayOf(Max, Regain).associateBy { it.name }

    object Max: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Mana
    }

    object Regain: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Mana

        fun getRegain(entity: LivingEntity, map: Map<DigitalParser.Type, DoubleArray>): Double {
            var regain = 0.0
            if (entity is Player) {
                map.forEach { (key, value) ->
                    regain += when(config.valueType) {
                        RANGE -> {
                            when (key) {
                                PERCENT -> Orryx.api().consumptionValueAPI.manaInstance.getMaxMana(entity).getNow(0.0) * random(value[0], value[1])
                                COUNT -> random(value[0], value[1])
                            }
                        }

                        SINGLE -> {
                            when (key) {
                                PERCENT -> Orryx.api().consumptionValueAPI.manaInstance.getMaxMana(entity).getNow(0.0) * value[0]
                                COUNT -> value[0]
                            }
                        }
                    }
                }
            }
            return regain
        }

        @SubscribeEvent
        private fun regain(e: OrryxPlayerManaEvents.Regain.Pre) {
            val map = e.player.attributeMemory()?.mergedAttribute(Regain) ?: return
            e.regainMana += getRegain(e.player, map)
        }
    }
}