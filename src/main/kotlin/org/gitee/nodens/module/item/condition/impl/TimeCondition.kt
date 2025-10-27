package org.gitee.nodens.module.item.condition.impl

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.module.item.condition.ICondition
import org.gitee.nodens.util.ConfigLazy
import taboolib.common5.cint
import java.time.LocalDateTime
import java.util.Timer

object TimeCondition: ICondition {

    override val keywords by ConfigLazy(Nodens.config) { getStringList("condition.time.keywords") }

    val pattern by ConfigLazy(Nodens.config) { getString("condition.time.pattern", "-")!! }

    override fun check(livingEntity: LivingEntity, itemStack: ItemStack, remain: String?, map: Map<String, String>): Boolean {
        remain ?: return true
        val time = remain.split(pattern).map { it.cint }
        val localTime = LocalDateTime.of(time[0], time[1], time[2], 0, 0)
        return localTime.isAfter(LocalDateTime.now())
    }
}