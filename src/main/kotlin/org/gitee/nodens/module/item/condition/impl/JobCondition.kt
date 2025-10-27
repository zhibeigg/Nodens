package org.gitee.nodens.module.item.condition.impl

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.module.item.condition.ICondition
import org.gitee.nodens.util.ConfigLazy
import org.gitee.nodens.util.getMap
import org.gitee.orryx.api.Orryx
import taboolib.common5.cbool
import taboolib.module.kether.orNull

object JobCondition: ICondition {

    override val keywords by ConfigLazy(Nodens.config) { getStringList("condition.job.keywords") }

    val pattern by ConfigLazy(Nodens.config) { getMap("condition.job.pattern") }

    override fun check(livingEntity: LivingEntity, itemStack: ItemStack, remain: String?, map: Map<String, String>): Boolean {
        remain ?: return true
        val job = pattern[remain] ?: return true
        return if (livingEntity is Player) {
            when {
                job.startsWith("orryx") -> {
                    Orryx.api().profileAPI.modifyProfile(livingEntity) {
                        it.job == job.drop(6)
                    }.orNull().cbool
                }
                else -> {
                    false
                }
            }
        } else {
            false
        }
    }
}