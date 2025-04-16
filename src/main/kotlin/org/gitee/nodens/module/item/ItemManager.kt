package org.gitee.nodens.module.item

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.module.item.generator.NormalGenerator
import org.gitee.nodens.util.files
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Configuration

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
    }

    private fun updateInventory(player: Player) {
        player.inventory.forEach {
            updateItem(player, it)
        }
    }

    private fun updateItem(player: Player?, old: ItemStack): ItemStack? {
        return NormalGenerator.update(player, old)
    }

}