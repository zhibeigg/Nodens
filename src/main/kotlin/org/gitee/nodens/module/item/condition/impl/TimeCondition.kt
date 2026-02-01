package org.gitee.nodens.module.item.condition.impl

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.module.item.condition.ICondition
import taboolib.common5.cint
import taboolib.module.configuration.util.ReloadAwareLazy
import java.time.LocalDateTime

object TimeCondition: ICondition {

    override val keywords by ReloadAwareLazy(Nodens.config) { Nodens.config.getStringList("condition.time.keywords") }

    val pattern by ReloadAwareLazy(Nodens.config) { Nodens.config.getString("condition.time.pattern", "-")!! }

    override fun check(livingEntity: LivingEntity, itemStack: ItemStack, remain: String?, map: Map<String, String>): Boolean {
        remain ?: return true
        val time = remain.split(pattern).map { it.cint }
        val localTime = LocalDateTime.of(time[0], time[1], time[2], 0, 0)
        return localTime.isAfter(LocalDateTime.now())
    }
}