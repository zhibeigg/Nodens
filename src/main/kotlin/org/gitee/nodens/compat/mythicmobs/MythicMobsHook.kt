package org.gitee.nodens.compat.mythicmobs

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDespawnEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.entityAttributeMemoriesMap
import taboolib.common.platform.event.SubscribeEvent

object MythicMobsHook {

    private const val ATTRIBUTE_TAG = "Nodens@MythicMobs"

    @SubscribeEvent
    private fun spawn(e: MythicMobSpawnEvent) {
        val attributes = Nodens.api().matchAttributes(e.mobType.config.getStringList("nodens"))
        entityAttributeMemoriesMap[e.entity.uniqueId] = EntityAttributeMemory(e.entity as? LivingEntity ?: return).apply {
            addAttribute(ATTRIBUTE_TAG, TempAttributeData(-1, attributes, true))
            syncAttributeToBukkit()
        }
    }

    @SubscribeEvent
    private fun despawn(e: MythicMobDespawnEvent) {

    }

    @SubscribeEvent
    private fun death(e: MythicMobDeathEvent) {

    }
}