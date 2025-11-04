package org.gitee.nodens.module.item.condition.impl

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.module.item.condition.ConditionManager.SLOT_DATA_KEY
import org.gitee.nodens.module.item.condition.ConditionManager.SLOT_IDENTIFY_KEY
import org.gitee.nodens.module.item.condition.ICondition
import org.gitee.nodens.util.ConfigLazy

object SlotCondition: ICondition {

    override val keywords by ConfigLazy { Nodens.config.getStringList("condition.slot.keywords") }

    val mainHand by ConfigLazy { Nodens.config.getStringList("condition.slot.pattern.main-hand") }
    val offHand by ConfigLazy { Nodens.config.getStringList("condition.slot.pattern.off-hand") }
    val helmet by ConfigLazy { Nodens.config.getStringList("condition.slot.pattern.helmet") }
    val chestplate by ConfigLazy { Nodens.config.getStringList("condition.slot.pattern.chestplate") }
    val leggings by ConfigLazy { Nodens.config.getStringList("condition.slot.pattern.leggings") }
    val boots by ConfigLazy { Nodens.config.getStringList("condition.slot.pattern.boots") }
    val dragoncoreSlots by ConfigLazy {
        Nodens.config.getConfigurationSection("condition.slot.pattern.dragoncore-slot")?.getKeys(false)?.associate { key ->
            key to Nodens.config.getStringList("condition.slot.pattern.dragoncore-slot.$key")
        }
    }

    override fun check(livingEntity: LivingEntity, itemStack: ItemStack, remain: String?, map: Map<String, String>): Boolean {
        remain ?: return false
        return when(map[SLOT_DATA_KEY]) {
            "main-hand" -> remain in mainHand
            "off-hand" -> remain in offHand
            "helmet" -> remain in helmet
            "chestplate" -> remain in chestplate
            "leggings" -> remain in leggings
            "boots" -> remain in boots
            "dragoncore" -> {
                dragoncoreSlots ?: return true
                val keys = dragoncoreSlots!![map[SLOT_IDENTIFY_KEY]] ?: return true
                remain in keys
            }

            else -> false
        }
    }
}