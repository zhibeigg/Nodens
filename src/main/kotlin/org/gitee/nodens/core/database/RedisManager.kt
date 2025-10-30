package org.gitee.nodens.core.database

import com.eatthepath.uuid.FastUUID
import com.gitee.redischannel.RedisChannelPlugin
import io.lettuce.core.HSetExArgs
import org.bukkit.entity.Player
import org.gitee.nodens.core.database.ISyncCache.Companion.GLOBAL_DROP
import taboolib.common5.cint
import java.util.concurrent.CompletableFuture

class RedisManager(private val exArgs: HSetExArgs): ISyncCache {

    private val api by lazy { RedisChannelPlugin.commandAPI() }

    override fun setDropTimes(player: Player, key: String, times: Int, global: Boolean) {
        api.useAsyncCommands { commands ->
            if (global) {
                commands.hsetex(GLOBAL_DROP, exArgs, mapOf(key to times.toString()))
            } else {
                commands.hsetex(FastUUID.toString(player.uniqueId), exArgs, mapOf(key to times.toString()))
            }
        }
    }

    override fun getDropTimes(player: Player, key: String, global: Boolean): CompletableFuture<Int> {
        return api.useAsyncCommands { commands ->
            if (global) {
                commands.hget(GLOBAL_DROP, key)
            } else {
                commands.hget(FastUUID.toString(player.uniqueId), key)
            }
        }.thenApply { value ->
            value.cint
        }
    }
}