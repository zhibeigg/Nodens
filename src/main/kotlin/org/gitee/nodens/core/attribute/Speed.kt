package org.gitee.nodens.core.attribute

import org.bukkit.attribute.Attribute
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.addBukkitAttribute

object Speed: IAttributeGroup {

    override val name: String = "Speed"

    override val numbers: Map<String, IAttributeGroup.Number> = arrayOf(Attack, Move).associateBy { it.name }

    object Attack: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = Speed

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            addBukkitAttribute(Attribute.GENERIC_ATTACK_SPEED, entitySyncProfile, valueMap)
        }
    }

    object Move: AbstractPercentNumber() {

        override val group: IAttributeGroup
            get() = Speed

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            addBukkitAttribute(Attribute.GENERIC_MOVEMENT_SPEED, entitySyncProfile, valueMap)
        }
    }
}