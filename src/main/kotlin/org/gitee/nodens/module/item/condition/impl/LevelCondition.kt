package org.gitee.nodens.module.item.condition.impl

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.module.item.condition.ICondition
import org.gitee.nodens.util.ConfigLazy
import taboolib.common5.cint

object LevelCondition: ICondition {

    override val keywords by ConfigLazy(Nodens.config) { getStringList("condition.level.keywords") }

    override fun check(livingEntity: LivingEntity, itemStack: ItemStack, remain: String?, map: Map<String, String>): Boolean {
        remain ?: return true
        return if (livingEntity is Player) {
            livingEntity.level >= remain.cint
        } else {
            false
        }
    }
}