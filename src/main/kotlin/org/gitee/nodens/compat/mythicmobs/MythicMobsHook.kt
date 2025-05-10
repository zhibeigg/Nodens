package org.gitee.nodens.compat.mythicmobs

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDespawnEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.entityAttributeMemoriesMap
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent

object MythicMobsHook {

    private const val ATTRIBUTE_TAG = "Nodens@MythicMobs"


    @Ghost
    @SubscribeEvent
    private fun spawn(e: MythicMobSpawnEvent) {
        val attributes = Nodens.api().matchAttributes(e.mobType.config.getStringList("Nodens"))
        entityAttributeMemoriesMap[e.entity.uniqueId] = EntityAttributeMemory(e.entity as? LivingEntity ?: return).apply {
            addAttribute(ATTRIBUTE_TAG, TempAttributeData(-1, attributes, true))
            syncAttributeToBukkit()
            e.mob.entity.health = e.mob.entity.maxHealth
        }
    }

    @Ghost
    @SubscribeEvent
    private fun despawn(e: MythicMobDespawnEvent) {
        entityAttributeMemoriesMap.remove(e.entity.uniqueId)
    }

    @Ghost
    @SubscribeEvent
    private fun death(e: MythicMobDeathEvent) {
        entityAttributeMemoriesMap.remove(e.entity.uniqueId)
        val drops = e.mob.type.config.getStringList("noDrops")

    }
}