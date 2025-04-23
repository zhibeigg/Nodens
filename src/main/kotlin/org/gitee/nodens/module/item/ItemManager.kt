package org.gitee.nodens.module.item

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.module.item.generator.NormalGenerator
import org.gitee.nodens.util.context
import org.gitee.nodens.util.files
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.configuration.Configuration
import taboolib.platform.util.isAir
import taboolib.platform.util.onlinePlayers

object ItemManager {

    val itemConfigs = mutableMapOf<String, ItemConfig>()

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
    }

    @SubscribeEvent
    private fun close(e: InventoryCloseEvent) {
        updateInventory(Bukkit.getPlayer(e.player.uniqueId) ?: return)
    }

    private fun updateInventory(player: Player) {
        player.inventory.forEach {
            if (it.isAir()) return@forEach
            val context = it.context<NormalGenerator.NormalContext>() ?: return@forEach
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