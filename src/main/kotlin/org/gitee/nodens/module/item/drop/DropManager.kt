package org.gitee.nodens.module.item.drop

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ItemMergeEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.common.PRDAlgorithm
import org.gitee.nodens.module.item.ItemManager
import org.gitee.nodens.module.item.generator.NormalGenerator
import org.gitee.nodens.util.ConfigLazy
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.random
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

object DropManager {

    val dropCancel by ConfigLazy(Nodens.config) { getBoolean("drop.cancel", false) }
    val dropSurvival by ConfigLazy(Nodens.config) { Duration.parse(getString("drop.survival", "P5M")!!).inWholeMilliseconds }

    val chanceMap: Cache<String, DropChance> = Caffeine.newBuilder()
        .initialCapacity(30)
        .maximumSize(100)
        .expireAfterAccess(60, TimeUnit.MINUTES)
        .build()

    @Schedule(false, period =  20)
    private fun clearDrops() {
        dropMap.forEach { (_, dropUser) ->
            dropUser.dropMap.removeIf {
                if (it.dead) {
                    if (it.item.isValid) {
                        it.item.remove()
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    val dropMap = mutableMapOf<UUID, DropUser>()

    fun drop(player: Player, location: Location, itemStack: ItemStack, dropSurvival: Long = DropManager.dropSurvival) {
        val item = location.world!!.dropItem(location, itemStack)
        item.customName = "${item.customName} * ${item.itemStack.amount}"
        dropMap.getOrPut(player.uniqueId) { DropUser(player.uniqueId) }.addItem(item, dropSurvival)
    }

    fun drop(player: Player, item: Item, dropSurvival: Long = DropManager.dropSurvival) {
        item.customName = "${item.customName} * ${item.itemStack.amount}"
        dropMap.getOrPut(player.uniqueId) { DropUser(player.uniqueId) }.addItem(item, dropSurvival)
    }

    fun tryDrop(player: Player, mob: String, item: String, percent: Double, location: Location, amount: Int, globalPrd: Boolean = false, map: Map<String, Any> = emptyMap()): Boolean {
        return if (percent > 0.5) {
            if (random(percent)) {
                val itemStack = NormalGenerator.generate(ItemManager.getItemConfig(item)!!, amount, player, map)
                drop(player, location, itemStack)
                true
            } else false
        } else {
            if (dropMap.getOrPut(player.uniqueId) { DropUser(player.uniqueId) }.hasDrop(mob, item, percent, globalPrd)) {
                val itemStack = NormalGenerator.generate(ItemManager.getItemConfig(item)!!, amount, player, map)
                drop(player, location, itemStack)
                true
            } else false
        }
    }

    @SubscribeEvent
    private fun merge(e: ItemMergeEvent) {
        if (e.isCancelled) return
        if (dropMap.any { it.value.dropMap.any { info -> info.item.uniqueId == e.entity.uniqueId || info.item.uniqueId == e.target.uniqueId } }) {
            e.isCancelled = true
        }
    }

    @SubscribeEvent
    private fun pickup(e: EntityPickupItemEvent) {
        if (e.isCancelled) return
        val player = e.entity as? Player ?: return
        val user = dropMap[player.uniqueId] ?: return
        val info = user.dropMap.firstOrNull { it.item.uniqueId == e.item.uniqueId }
        val remaining = e.remaining
        if (info != null) {
            if (remaining <= 0) {
                user.dropMap.remove(info)
            }
        } else {
            e.isCancelled = true
        }
    }

    @SubscribeEvent
    private fun player(e: PlayerDropItemEvent) {
        if (e.isCancelled) return
        if (dropCancel) {
            e.isCancelled = true
        } else {
            drop(e.player, e.itemDrop)
        }
    }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        dropMap.remove(e.player.uniqueId)
    }
}