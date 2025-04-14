package org.gitee.nodens.common

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.core.IAttributeGroup
import taboolib.common.platform.function.info

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
    }

    fun resetHealth() {
        val max = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: return
        if (entity.health > max) {
            entity.health = max
        }
    }
}