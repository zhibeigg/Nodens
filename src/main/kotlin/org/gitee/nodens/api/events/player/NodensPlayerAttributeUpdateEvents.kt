package org.gitee.nodens.api.events.player

import org.gitee.nodens.core.entity.EntityAttributeMemory
import taboolib.platform.type.BukkitProxyEvent

class NodensPlayerAttributeUpdateEvents {

    class Pre(val entityAttributeMemory: EntityAttributeMemory) : BukkitProxyEvent()

    /**
     * 此事件为异步事件
     * */
    class Post(val entityAttributeMemory: EntityAttributeMemory) : BukkitProxyEvent() {
        override val allowCancelled: Boolean
            get() = false
    }
}