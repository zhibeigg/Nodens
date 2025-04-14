package org.gitee.nodens.core.attribute

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.NODENS_NAMESPACE
import org.gitee.nodens.util.addBukkitAttribute

object Speed: IAttributeGroup {

    override val name: String = "Speed"

    object Attack: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(Speed.name, name)

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            addBukkitAttribute(Speed, Attribute.GENERIC_ATTACK_SPEED, entitySyncProfile, valueMap)
        }
    }

    object Move: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(Speed.name, name)

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            addBukkitAttribute(Speed, Attribute.GENERIC_MOVEMENT_SPEED, entitySyncProfile, valueMap)
        }
    }

}