package org.gitee.nodens.module.item.drop

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import eos.moe.dragoncore.api.event.EntityJoinWorldEvent
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ItemMergeEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.module.item.ItemManager
import org.gitee.nodens.module.item.generator.NormalGenerator
import org.gitee.nodens.util.ConfigLazy
import org.gitee.nodens.util.GlowAPIPlugin
import org.inventivetalent.glow.GlowAPI
import taboolib.common.platform.Ghost
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.random
import taboolib.module.nms.getName
import taboolib.platform.util.hasName
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

object DropManager {

    val dropCancel by ConfigLazy { Nodens.config.getBoolean("drop.cancel", false) }
    val dropSurvival by ConfigLazy { Duration.parse(Nodens.config.getString("drop.survival", "P5M")!!).inWholeMilliseconds }

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
        updateGlow(player, item)
        showName(player, item)
        dropMap.getOrPut(player.uniqueId) { DropUser(player.uniqueId) }.addItem(item, dropSurvival)
    }

    fun drop(player: Player, item: Item, dropSurvival: Long = DropManager.dropSurvival) {
        updateGlow(player, item)
        showName(player, item)
        dropMap.getOrPut(player.uniqueId) { DropUser(player.uniqueId) }.addItem(item, dropSurvival)
    }

    fun showName(player: Player, item: Item) {
        if (item.itemStack.hasName()) {
            item.isCustomNameVisible = true
            item.customName = "${item.itemStack.getName(player)} * ${item.itemStack.amount}"
        }
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

    fun updateGlow(player: Player, item: Item) {
        if (!GlowAPIPlugin.isEnabled) return
        player.world.players.forEach {
            if (it == player) {
                GlowAPI.setGlowing(item, GlowAPI.Color.GREEN, it)
            } else {
                GlowAPI.setGlowing(item, GlowAPI.Color.RED, it)
            }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun join(e: EntityJoinWorldEvent) {
        dropMap.forEach { (key, user) ->
            val item = user.dropMap.firstOrNull { info ->
                info.item.uniqueId == e.entityUUID
            }
            if (!GlowAPIPlugin.isEnabled && item != null) {
                if (key == e.player.uniqueId) {
                    GlowAPI.setGlowing(item.item, GlowAPI.Color.GREEN, e.player)
                } else {
                    GlowAPI.setGlowing(item.item, GlowAPI.Color.RED, e.player)
                }
            }
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