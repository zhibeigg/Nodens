package org.gitee.nodens.module.item

import eos.moe.armourers.api.DragonAPI
import eos.moe.armourers.api.PlayerSkinUpdateEvent
import eos.moe.dragoncore.api.SlotAPI
import eos.moe.dragoncore.database.IDataBase
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.api.events.item.NodensItemUpdateEvents
import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.module.item.generator.NormalGenerator
import org.gitee.nodens.module.ui.ItemConfigManagerUI
import org.gitee.nodens.util.ConfigLazy
import org.gitee.nodens.util.context
import org.gitee.nodens.util.files
import org.gitee.nodens.util.getConfig
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.unsafeLazy
import taboolib.module.configuration.Configuration
import taboolib.platform.util.isAir
import taboolib.platform.util.onlinePlayers
import java.util.*

object ItemManager {

    val itemConfigs = mutableMapOf<String, ItemConfig>()
    private val heldItemArmourersMap by unsafeLazy { mutableMapOf<UUID, List<String>?>() }

    val dragoncoreSlots by ConfigLazy(Nodens.config) { Nodens.config.getStringList("update-dragoncore-slots") }

    @Reload(0)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        itemConfigs.clear()
        files("items", "example.yml") {
            val configuration = Configuration.loadFromFile(it)
            configuration.getKeys(false).forEach { key ->
                itemConfigs[key] = ItemConfig(key, configuration.getConfigurationSection(key)!!)
            }
        }
        onlinePlayers.forEach { player ->
            updateInventory(player)
        }
        ItemConfigManagerUI.load()
    }

    @SubscribeEvent
    private fun held(e: PlayerItemHeldEvent) {
        updateSkin(e.player)
    }

    @SubscribeEvent
    private fun drag(e: PlayerSwapHandItemsEvent) {
        updateSkin(e.player)
    }

    @Ghost
    @SubscribeEvent
    private fun updateArmourers(e: PlayerSkinUpdateEvent) {
        e.skinList.addAll(heldItemArmourersMap[e.player.uniqueId] ?: return)
    }

    @SubscribeEvent
    private fun close(e: InventoryCloseEvent) {
        val player = Bukkit.getPlayer(e.player.uniqueId) ?: return
        updateInventory(player)
        updateSkin(player)
    }

    @SubscribeEvent
    private fun join(e: PlayerJoinEvent) {
        dragoncoreSlots.forEach {
            SlotAPI.getSlotItem(e.player, it, object : IDataBase.Callback<ItemStack> {

                override fun onResult(item: ItemStack?) {
                    if (item.isAir()) return
                    val context = item.context() ?: return
                    val config = getItemConfig(context.key) ?: return
                    if (config.isUpdate && config.hashCode != context.hashcode) {
                        val new = updateItem(e.player, item)
                        SlotAPI.setSlotItem(e.player, it, new, false)
                        NodensItemUpdateEvents.Post(item, new).call()
                    }
                }

                override fun onFail() {
                }
            })
        }
        updateBukkitInventory(e.player)
        updateSkin(e.player)
    }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        heldItemArmourersMap.remove(e.player.uniqueId)
    }

    fun updateSkin(player: Player) {
        val list = mutableListOf<String>()
        player.inventory.itemInMainHand.getConfig()?.armourers?.let { list += it }
        player.inventory.itemInOffHand.getConfig()?.armourers?.let { list += it }
        player.inventory.armorContents.forEach { item ->
            item.getConfig()?.armourers?.let { list += it }
        }
        heldItemArmourersMap[player.uniqueId] = list
        DragonAPI.updatePlayerSkin(player)
    }

    private fun updateInventory(player: Player) {
        dragoncoreSlots.forEach {
            val item = SlotAPI.getCacheSlotItem(player, it)
            if (item.isAir()) return
            val context = item.context() ?: return
            val config = getItemConfig(context.key) ?: return
            if (config.isUpdate && config.hashCode != context.hashcode) {
                val new = updateItem(player, item)
                SlotAPI.setSlotItem(player, it, new, false)
                NodensItemUpdateEvents.Post(item, new).call()
            }
        }
        updateBukkitInventory(player)
    }

    private fun updateBukkitInventory(player: Player) {
        player.inventory.contents.forEachIndexed { index, item ->
            if (item.isAir()) return@forEachIndexed
            val context = item.context() ?: return@forEachIndexed
            val config = getItemConfig(context.key) ?: return@forEachIndexed
            if (config.isUpdate && config.hashCode != context.hashcode) {
                val new = updateItem(player, item)
                player.inventory.setItem(index, new)
                NodensItemUpdateEvents.Post(item, new).call()
            }
        }
    }

    private fun updateItem(player: Player?, old: ItemStack): ItemStack? {
        return NormalGenerator.update(player, old)
    }

    fun getItemConfig(key: String): ItemConfig? {
        return itemConfigs[key]
    }
}