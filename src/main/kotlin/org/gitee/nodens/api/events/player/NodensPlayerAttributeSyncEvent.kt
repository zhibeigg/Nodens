package org.gitee.nodens.api.events.player

import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.IAttributeGroup
import taboolib.platform.type.BukkitProxyEvent

class NodensPlayerAttributeSyncEvent(val entitySyncProfile: EntitySyncProfile, val attributeData: Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>) : BukkitProxyEvent()