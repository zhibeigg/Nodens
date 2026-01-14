package org.gitee.nodens.core.database

import com.gitee.redischannel.RedisChannelPlugin
import com.gitee.redischannel.RedisChannelPlugin.Type.CLUSTER
import com.gitee.redischannel.RedisChannelPlugin.Type.SINGLE
import com.gitee.redischannel.api.events.ClientStartEvent
import io.lettuce.core.HSetExArgs
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import java.time.Duration
import java.util.concurrent.CompletableFuture

interface ISyncCache {

    companion object {

        const val GLOBAL_DROP = "nodens@global@drop"

        lateinit var INSTANCE: ISyncCache

        @Ghost
        @SubscribeEvent
        private fun loadCache(e: ClientStartEvent) {
            if (org.gitee.nodens.util.RedisChannelPlugin.isEnabled) {
                INSTANCE = when(RedisChannelPlugin.type) {
                    CLUSTER -> RedisClusterManager(HSetExArgs().ex(Duration.ofHours(3)))
                    SINGLE -> RedisManager(HSetExArgs().ex(Duration.ofHours(3)))
                    null -> error("Redis 炸了")
                }
            }
        }

        @Awake(LifeCycle.ENABLE)
        private fun enable() {
            if (org.gitee.nodens.util.RedisChannelPlugin.isEnabled) {
                INSTANCE = JSONManager()
            }
        }
    }

    fun setDropTimes(player: Player, key: String, times: Int, global: Boolean = false)

    fun getDropTimes(player: Player, key: String, global: Boolean = false): CompletableFuture<Int>
}