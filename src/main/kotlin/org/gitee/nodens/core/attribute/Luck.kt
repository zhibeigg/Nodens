package org.gitee.nodens.core.attribute

import org.bukkit.attribute.Attribute
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.addBukkitAttribute

object Luck: IAttributeGroup {

    override val name: String = "Luck"

    override val numbers: Map<String, IAttributeGroup.Number> = arrayOf(Max).associateBy { it.name }

    object Max: AbstractSyncNumber() {

        override val group: IAttributeGroup
            get() = Luck

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            addBukkitAttribute(Attribute.GENERIC_LUCK, entitySyncProfile, valueMap)
        }
    }
}
