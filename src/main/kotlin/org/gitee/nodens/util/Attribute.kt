package org.gitee.nodens.util

import net.minecraft.server.v1_12_R1.GenericAttributes
import org.bukkit.attribute.Attribute
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.IAttributeGroup

fun IAttributeGroup.Number.addBukkitAttribute(attribute: Attribute, entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    ensureSync {
        var value = 0.0
        valueMap.forEach { (type, double) ->
            value += when(type) {
                PERCENT -> ((valueMap[COUNT]?.get(0) ?: 0.0) + (entitySyncProfile.entity.getAttribute(attribute)?.defaultValue ?: 0.0)) * double[0]
                COUNT -> double[0]
            }
        }
        entitySyncProfile.addModifier(
            this,
            EntitySyncProfile.PriorityModifier(
                GenericAttributes.maxHealth,
                value,
                config.syncPriority
            )
        )
    }
}