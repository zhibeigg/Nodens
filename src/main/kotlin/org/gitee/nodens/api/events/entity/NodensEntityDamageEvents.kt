package org.gitee.nodens.api.events.entity

import org.gitee.nodens.common.DamageProcessor
import taboolib.platform.type.BukkitProxyEvent

class NodensEntityDamageEvents {

    class Pre(val processor: DamageProcessor): BukkitProxyEvent()

    class Post(val damage: Double, val processor: DamageProcessor): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}