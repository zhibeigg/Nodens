package org.gitee.nodens.common

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.maxHealth

class EntitySyncProfile(val entity: LivingEntity) {

    private val modifierMap = hashMapOf<IAttributeGroup.Number, PriorityModifier>()

    class PriorityModifier(val attribute: Attribute, val number: IAttributeGroup.Number, val value: Double, val priority: Int) {

        fun apply(entity: LivingEntity) {
            val attributeInstant = entity.getAttribute(attribute) ?: return
            val name = "${number.group.name}${number.name}"
            val modifier = attributeInstant.modifiers.firstOrNull { it.name == name }
            if (modifier?.amount != value) {
                modifier?.also {
                    attributeInstant.removeModifier(it)
                }
                attributeInstant.addModifier(
                    AttributeModifier(name, value, AttributeModifier.Operation.ADD_NUMBER)
                )
            }
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