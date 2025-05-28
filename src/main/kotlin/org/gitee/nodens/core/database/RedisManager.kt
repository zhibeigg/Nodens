package org.gitee.nodens.core.database

import com.eatthepath.uuid.FastUUID
import com.gitee.redischannel.RedisChannelPlugin
import com.gitee.redischannel.util.proxyAsyncCommand
import io.lettuce.core.HSetExArgs
import org.bukkit.entity.Player
import taboolib.common5.cint
import java.time.Duration
import java.util.concurrent.CompletableFuture

object RedisManager {

    private val exArgs = HSetExArgs().ex(Duration.ofHours(3))
    const val GLOBAL_DROP = "nodens@global@drop"

    fun setDropTimes(player: Player, key: String, times: Int, global: Boolean = false) {
        RedisChannelPlugin.api.proxyAsyncCommand().thenAccept {
            if (global) {
                it.hsetex(GLOBAL_DROP, exArgs, mapOf(key to times.toString()))
            } else {
                it.hsetex(FastUUID.toString(player.uniqueId), exArgs, mapOf(key to times.toString()))
            }
        }
    }

    fun getDropTimes(player: Player, key: String, global: Boolean = false): CompletableFuture<Int> {
        return RedisChannelPlugin.api.proxyAsyncCommand().thenCompose {
            if (global) {
                it.hget(GLOBAL_DROP, key)
            } else {
                it.hget(FastUUID.toString(player.uniqueId), key)
            }.thenApply { value ->
                value.cint
            }
        }
    }
}