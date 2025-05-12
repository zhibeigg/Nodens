package org.gitee.nodens.core.database

import com.eatthepath.uuid.FastUUID
import com.gitee.redischannel.RedisChannelPlugin
import org.bukkit.entity.Player
import taboolib.common5.cint
import java.util.concurrent.CompletableFuture

object RedisManager {

    const val SECOND_3_HOURS = 3 * 60 * 60L
    const val GLOBAL_DROP = "nodens@global@drop"

    fun setDropTimes(player: Player, key: String, times: Int, global: Boolean = false) {
        if (global) {
            RedisChannelPlugin.api.hSet(GLOBAL_DROP, key, times.toString(), SECOND_3_HOURS, true)
        } else {
            RedisChannelPlugin.api.hSet(FastUUID.toString(player.uniqueId), key, times.toString(), SECOND_3_HOURS, true)
        }
    }

    fun getDropTimes(player: Player, key: String, global: Boolean = false): CompletableFuture<Int> {
        return if (global) {
            RedisChannelPlugin.api.hAsyncGet(GLOBAL_DROP, key)
        } else {
            RedisChannelPlugin.api.hAsyncGet(FastUUID.toString(player.uniqueId), key)
        }.thenApply {
            it.cint
        }
    }
}