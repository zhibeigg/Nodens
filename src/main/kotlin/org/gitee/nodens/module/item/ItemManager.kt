package org.gitee.nodens.module.item

import eos.moe.armourers.api.DragonAPI
import eos.moe.armourers.api.PlayerSkinUpdateEvent
import eos.moe.dragoncore.api.SlotAPI
import eos.moe.dragoncore.database.IDataBase
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
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
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy
import taboolib.module.configuration.Configuration
import taboolib.platform.util.isAir
import taboolib.platform.util.onlinePlayers
import java.util.UUID

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
        heldItemArmourersMap[e.player.uniqueId] = e.player.inventory.getItem(e.newSlot)?.getConfig()?.armourers
        DragonAPI.updatePlayerSkin(e.player)
    }

    @Ghost
    @SubscribeEvent
    private fun updateArmourers(e: PlayerSkinUpdateEvent) {
        e.skinList.addAll(heldItemArmourersMap[e.player.uniqueId] ?: return)
    }

    @SubscribeEvent
    private fun close(e: InventoryCloseEvent) {
        updateInventory(Bukkit.getPlayer(e.player.uniqueId) ?: return)
    }

    @SubscribeEvent
    private fun join(e: PlayerJoinEvent) {
        dragoncoreSlots.forEach {
            SlotAPI.getSlotItem(e.player, it, object : IDataBase.Callback<ItemStack> {

                override fun onResult(p0: ItemStack?) {
                    if (p0.isAir()) return
                    val context = p0.context() ?: return
                    val config = getItemConfig(context.key) ?: return
                    if (config.isUpdate && config.hashCode == context.hashcode) {
                        SlotAPI.setSlotItem(e.player, it, updateItem(e.player, p0), false)
                    }
                }

                override fun onFail() {
                }
            })
        }
    }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        heldItemArmourersMap.remove(e.player.uniqueId)
    }

    private fun updateInventory(player: Player) {
        dragoncoreSlots.forEach {
            val item = SlotAPI.getCacheSlotItem(player, it)
            if (item.isAir()) return
            val context = item.context() ?: return
            val config = getItemConfig(context.key) ?: return
            if (config.isUpdate && config.hashCode == context.hashcode) {
                SlotAPI.setSlotItem(player, it, updateItem(player, item), false)
            }
        }
        player.inventory.forEach {
            if (it.isAir()) return@forEach
            val context = it.context() ?: return@forEach
            val config = getItemConfig(context.key) ?: return@forEach
            if (config.isUpdate && config.hashCode == context.hashcode) updateItem(player, it)
        }
    }

    private fun updateItem(player: Player?, old: ItemStack): ItemStack? {
        return NormalGenerator.update(player, old)
    }

    fun getItemConfig(key: String): ItemConfig? {
        return itemConfigs[key]
    }
}