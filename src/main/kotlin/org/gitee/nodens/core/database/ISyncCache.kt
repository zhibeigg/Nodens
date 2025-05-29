package org.gitee.nodens.core.database

import com.gitee.redischannel.RedisChannelPlugin
import com.gitee.redischannel.RedisChannelPlugin.Type.*
import io.lettuce.core.HSetExArgs
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import java.time.Duration
import java.util.concurrent.CompletableFuture

interface ISyncCache {

    companion object {

        internal val exArgs = HSetExArgs().ex(Duration.ofHours(3))
        const val GLOBAL_DROP = "nodens@global@drop"

        lateinit var INSTANCE: ISyncCache

        @Awake(LifeCycle.ENABLE)
        private fun init() {
            INSTANCE = when(RedisChannelPlugin.type) {
                CLUSTER -> RedisClusterManager()
                SINGLE -> RedisManager()
            }
        }
    }

    fun setDropTimes(player: Player, key: String, times: Int, global: Boolean = false)

    fun getDropTimes(player: Player, key: String, global: Boolean = false): CompletableFuture<Int>
}