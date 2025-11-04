package org.gitee.nodens.module.item.condition.impl

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.module.item.condition.ICondition
import org.gitee.nodens.util.ConfigLazy

object BindCondition: ICondition {

    override val keywords by ConfigLazy { Nodens.config.getStringList("condition.bind.keywords") }

    override fun check(livingEntity: LivingEntity, itemStack: ItemStack, remain: String?, map: Map<String, String>): Boolean {
        remain ?: return true
        return livingEntity.name == remain
    }
}