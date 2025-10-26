package org.gitee.nodens.module.item.condition

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

interface ICondition {

    val keywords: List<String>

    fun check(livingEntity: LivingEntity, itemStack: ItemStack, remain: String, map: Map<String, String> = emptyMap()): Boolean
}