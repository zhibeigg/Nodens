package org.gitee.nodens.module.item.drop

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.gitee.nodens.core.database.ISyncCache
import org.gitee.nodens.core.database.RedisManager
import java.util.*
import java.util.concurrent.TimeUnit

class DropUser(val uuid: UUID) {

    val player: Player
        get() = Bukkit.getPlayer(uuid)!!

    val dropMap = mutableListOf<Info>()
    val chanceMap: Cache<String, DropChance> = Caffeine.newBuilder()
        .initialCapacity(30)
        .maximumSize(100)
        .expireAfterAccess(60, TimeUnit.MINUTES)
        .build()

    class Info(val item: Item, val dropSurvival: Long) {

        val init = System.currentTimeMillis()

        val survival
            get() = System.currentTimeMillis() - init

        val countdown
            get() = (dropSurvival - survival).coerceAtLeast(0)

        val dead: Boolean
            get() = survival > dropSurvival
    }

    fun addItem(item: Item, dropSurvival: Long) {

        dropMap += Info(item, dropSurvival)
    }

    fun hasDrop(mob: String, item: String, percent: Double, global: Boolean = false): Boolean {

        val key = "$mob@$item"
        var set = true

        val cache = if (global) {
            DropManager.chanceMap
        } else {
            chanceMap
        }

        val chance = cache.get(key) {
            DropChance(percent).apply {
                set = false
                ISyncCache.INSTANCE.getDropTimes(player, key, global).thenAccept {
                    times = it
                }
            }
        }

        val drop = chance!!.hasDrop()
        if (set) {
            ISyncCache.INSTANCE.setDropTimes(player, key, chance.times, global)
        }

        return drop
    }
}