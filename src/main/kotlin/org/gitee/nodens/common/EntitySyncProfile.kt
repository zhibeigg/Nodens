package org.gitee.nodens.common

import net.minecraft.server.v1_12_R1.GenericAttributes
import net.minecraft.server.v1_12_R1.IAttribute
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.maxHealth

class EntitySyncProfile(val entity: LivingEntity) {

    private val modifierMap = hashMapOf<IAttributeGroup.Number, PriorityModifier>()

    class PriorityModifier(val attribute: IAttribute, val value: Double, val priority: Int) {

        fun apply(entity: LivingEntity) {
            (entity as CraftPlayer).handle.getAttributeInstance(attribute).value = value
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