package org.gitee.nodens.api.events.player

import org.gitee.nodens.core.entity.EntityAttributeMemory
import taboolib.platform.type.BukkitProxyEvent

class NodensPlayerAttributeUpdateEvents {

    class Pre(val entityAttributeMemory: EntityAttributeMemory) : BukkitProxyEvent()

    class Post(val entityAttributeMemory: EntityAttributeMemory) : BukkitProxyEvent() {
        override val allowCancelled: Boolean
            get() = false
    }
}