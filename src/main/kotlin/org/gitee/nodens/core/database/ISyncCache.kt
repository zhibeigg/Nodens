package org.gitee.nodens.core.database

import com.gitee.redischannel.RedisChannelPlugin
import com.gitee.redischannel.RedisChannelPlugin.Type.CLUSTER
import com.gitee.redischannel.RedisChannelPlugin.Type.SINGLE
import io.lettuce.core.HSetExArgs
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.platform.bukkit.Parallel
import java.time.Duration
import java.util.concurrent.CompletableFuture

interface ISyncCache {

    companion object {

        const val GLOBAL_DROP = "nodens@global@drop"

        lateinit var INSTANCE: ISyncCache

        @Parallel(dependOn = ["redis_channel"], runOn = LifeCycle.ENABLE)
        private fun init() {
            INSTANCE = if (org.gitee.nodens.util.RedisChannelPlugin.isEnabled) {
                when(RedisChannelPlugin.type) {
                    CLUSTER -> RedisClusterManager(HSetExArgs().ex(Duration.ofHours(3)))
                    SINGLE -> RedisManager(HSetExArgs().ex(Duration.ofHours(3)))
                    null -> error("Redis 炸了")
                }
            } else {
                JSONManager()
            }
        }
    }

    fun setDropTimes(player: Player, key: String, times: Int, global: Boolean = false)

    fun getDropTimes(player: Player, key: String, global: Boolean = false): CompletableFuture<Int>
}