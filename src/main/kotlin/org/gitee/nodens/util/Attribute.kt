package org.gitee.nodens.util

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.IAttributeGroup

fun IAttributeGroup.Number.addBukkitAttribute(group: IAttributeGroup, attribute: Attribute, entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    ensureSync {
        var value = 0.0
        valueMap.forEach { (type, double) ->
            value += when(type) {
                PERCENT -> ((valueMap[COUNT]?.get(0) ?: 0.0) + (entitySyncProfile.entity.getAttribute(attribute)?.baseValue ?: 0.0)) * double[0]
                COUNT -> double[0]
            }
        }
        entitySyncProfile.addModifier(
            this,
            EntitySyncProfile.PriorityModifier(
                attribute,
                AttributeModifier(
                    "$NODENS_NAMESPACE${group.name}${name}",
                    value,
                    AttributeModifier.Operation.ADD_NUMBER
                ),
                config.syncPriority
            )
        )
    }
}