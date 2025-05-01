package org.gitee.nodens.api.events.entity

import org.gitee.nodens.common.RegainProcessor
import taboolib.platform.type.BukkitProxyEvent

class NodensEntityRegainEvents {

    class Pre(val processor: RegainProcessor): BukkitProxyEvent()

    class Post(val regain: Double, val processor: RegainProcessor): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}