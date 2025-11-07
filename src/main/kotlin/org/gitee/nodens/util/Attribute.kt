package org.gitee.nodens.util

import org.bukkit.attribute.Attribute
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.attribute.ISyncDefault

fun IAttributeGroup.Number.addBukkitAttribute(attribute: Attribute, entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    ensureSync {
        val default = (this as? ISyncDefault)?.default ?: return@ensureSync
        var value = 0.0
        valueMap.forEach { (type, double) ->
            value += when (type) {
                PERCENT -> ((valueMap[COUNT]?.get(0) ?: 0.0) + default) * double[0]
                COUNT -> double[0]
            }
        }
        entitySyncProfile.addModifier(
            this,
            EntitySyncProfile.PriorityModifier(
                attribute,
                this,
                value + default,
                config.syncPriority
            )
        )
    }
}