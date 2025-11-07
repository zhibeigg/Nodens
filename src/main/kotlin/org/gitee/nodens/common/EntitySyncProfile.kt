package org.gitee.nodens.common

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.maxHealth
import taboolib.common.platform.function.warning

class EntitySyncProfile(val entity: LivingEntity) {

    private val modifierMap = hashMapOf<IAttributeGroup.Number, PriorityModifier>()

    class PriorityModifier(val attribute: Attribute, val number: IAttributeGroup.Number, val value: Double, val priority: Int, val setValue: Boolean = true, val runnable: () -> Unit = {}) {

        fun apply(entity: LivingEntity) {
            if (setValue) {
                val attributeInstant = entity.getAttribute(attribute) ?: return
                try {
                    if (attribute == Attribute.GENERIC_MOVEMENT_SPEED) {
                        val player = entity as? Player ?: return
                        player.walkSpeed = value.toFloat()
                    } else {
                        attributeInstant.baseValue = value
                    }
                } catch (e: IllegalArgumentException) {
                    warning(e.message)
                }
            }
            runnable()
        }
    }

    fun addModifier(attribute: IAttributeGroup.Number, modifier: PriorityModifier) {
        modifierMap[attribute] = modifier
    }

    fun applyModifiers() {
        modifierMap.values.sortedBy { it.priority }.forEach {
            it.apply(entity)
        }
        if (AttributeManager.healthScaled && entity is Player) {
            entity.isHealthScaled = true
            entity.healthScale = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.defaultValue / entity.maxHealth()
        }
    }

    fun resetHealth() {
        val max = entity.maxHealth()
        if (entity.health > max) {
            entity.health = max
        }
    }
}