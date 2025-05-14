package org.gitee.nodens.common

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.maxHealth

class EntitySyncProfile(val entity: LivingEntity) {

    private val modifierMap = hashMapOf<IAttributeGroup.Number, PriorityModifier>()

    class PriorityModifier(val attribute: Attribute, val modifier: AttributeModifier, val priority: Int) {

        fun remove(entity: LivingEntity) {
            entity.getAttribute(attribute)?.removeModifier(modifier)
        }

        fun apply(entity: LivingEntity) {
            entity.getAttribute(attribute)?.addModifier(modifier)
        }
    }
    
    fun clearModifiers() {
        modifierMap.values.sortedByDescending { it.priority }.forEach {
            it.remove(entity)
        }
        modifierMap.clear()
    }

    fun addModifier(attribute: IAttributeGroup.Number, modifier: PriorityModifier) {
        modifierMap[attribute] = modifier
    }

    fun applyModifiers() {
        modifierMap.values.sortedBy { it.priority }.forEach {
            it.apply(entity)
        }
        if (entity is Player) {
            entity.healthScale = 20.0 / (entity.health / entity.maxHealth())
        }
    }

    fun resetHealth() {
        val max = entity.maxHealth()
        if (entity.health > max) {
            entity.health = max
        }
    }
}