package org.gitee.nodens.module.item

import eos.moe.armourers.api.DragonAPI
import eos.moe.armourers.api.PlayerSkinUpdateEvent
import eos.moe.dragoncore.api.FutureSlotAPI
import eos.moe.dragoncore.api.SlotAPI
import eos.moe.dragoncore.database.IDataBase
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
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
import org.gitee.nodens.util.DragonCorePlugin
import org.gitee.nodens.util.context
import org.gitee.nodens.util.files
import org.gitee.nodens.util.getConfig
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.unsafeLazy
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import taboolib.platform.util.isAir
import taboolib.platform.util.onlinePlayers
import java.util.*

object ItemManager {

    val itemConfigs = mutableMapOf<String, ItemConfig>()
    private val heldItemArmourersMap by unsafeLazy { mutableMapOf<UUID, List<String>?>() }
    private val enableArmourers by unsafeLazy { Bukkit.getPluginManager().isPluginEnabled("DragonArmourers") }

    private val dragonCoreIsEnabled by unsafeLazy { DragonCorePlugin.isEnabled }

    val dragoncoreSlots by ConfigLazy(Nodens.config) { getStringList("update-dragoncore-slots") }

    @Reload(0)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        itemConfigs.clear()
        val cloneMap = hashMapOf<String, ConfigurationSection>()
        files("items", "example.yml") {
            val configuration = Configuration.loadFromFile(it)
            configuration.getKeys(false).forEach { key ->
                val configurationSection = configuration.getConfigurationSection(key)!!
                if (configurationSection.contains("clone")) {
                    cloneMap[key] = configurationSection
                } else {
                    itemConfigs[key] = ItemConfig(key, configurationSection)
                }
            }
        }
        cloneMap.forEach {
            val clone = it.value.getString("clone") ?: return@forEach
            itemConfigs[it.key] = CloneItemConfig(it.key, itemConfigs[clone] ?: return@forEach, it.value)
        }
        onlinePlayers.forEach { player ->
            updateInventory(player)
        }
        ItemConfigManagerUI.load()
    }

    @SubscribeEvent
    private fun held(e: PlayerItemHeldEvent) {
        if (!enableArmourers) return
        val list = mutableListOf<String>()
        e.player.inventory.getItem(e.newSlot)?.getConfig()?.armourers?.let { list += it }
        e.player.inventory.itemInOffHand.getConfig()?.armourers?.let { list += it }
        e.player.inventory.armorContents.forEach { item ->
            item?.getConfig()?.armourers?.let { list += it }
        }
        heldItemArmourersMap[e.player.uniqueId] = list
        DragonAPI.updatePlayerSkin(e.player)
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
        if (dragonCoreIsEnabled) {
            dragoncoreSlots.forEach {
                FutureSlotAPI.getSlotItem(e.player, it).thenAccept { item ->
                    if (item.isAir()) return@thenAccept
                    val context = item.context() ?: return@thenAccept
                    val config = getItemConfig(context.key) ?: return@thenAccept
                    if (config.isUpdate && config.hashCode != context.hashcode) {
                        val new = updateItem(e.player, item)
                        SlotAPI.setSlotItem(e.player, it, new, false)
                        NodensItemUpdateEvents.Post(item, new).call()
                    }
                }
            }
        }
        updateBukkitInventory(e.player)
        updateSkin(e.player)
    }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        heldItemArmourersMap.remove(e.player.uniqueId)
    }

    fun updateSkin(player: Player) {
        if (!enableArmourers) return
        val list = mutableListOf<String>()
        player.inventory.itemInMainHand.getConfig()?.armourers?.let { list += it }
        player.inventory.itemInOffHand.getConfig()?.armourers?.let { list += it }
        player.inventory.armorContents.forEach { item ->
            item?.getConfig()?.armourers?.let { list += it }
        }
        heldItemArmourersMap[player.uniqueId] = list
        DragonAPI.updatePlayerSkin(player)
    }

    private fun updateInventory(player: Player) {
        if (dragonCoreIsEnabled) {
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
        player.updateInventory()
    }

    private fun updateItem(player: Player?, old: ItemStack): ItemStack? {
        return NormalGenerator.update(player, old)
    }

    fun getItemConfig(key: String): ItemConfig? {
        return itemConfigs[key]
    }
}