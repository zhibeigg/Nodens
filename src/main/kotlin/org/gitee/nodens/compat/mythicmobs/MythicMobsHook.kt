package org.gitee.nodens.compat.mythicmobs

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDespawnEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.compat.mythicmobs.mechanic.MythicMobsDamageMechanic
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent

object MythicMobsHook {

    @Ghost
    @Awake(LifeCycle.ENABLE)
    private fun loadActive() {
        if (!MythicMobsCompatSupport.isLegacyMythicMobs) return
        MythicMobs.inst().mobManager.activeMobs.forEach {
            val entity = it.entity.bukkitEntity as? LivingEntity ?: return@forEach
            MythicMobsCompatSupport.attachAttributes(entity, it.type.config.getStringList("Nodens"), false)
        }
    }

    @Ghost
    @SubscribeEvent
    private fun registerMechanic(event: MythicMechanicLoadEvent) {
        if (!MythicMobsCompatSupport.isLegacyMythicMobs) return
        when (event.mechanicName.uppercase()) {
            "NO-DAMAGE" -> event.register(MythicMobsDamageMechanic(event.container.configLine, event.config))
        }
    }

    @Ghost
    @SubscribeEvent
    private fun spawn(e: MythicMobSpawnEvent) {
        if (!MythicMobsCompatSupport.isLegacyMythicMobs) return
        val entity = e.entity as? LivingEntity ?: return
        MythicMobsCompatSupport.attachAttributes(entity, e.mobType.config.getStringList("Nodens"), true)
        e.mob.entity.health = e.mob.entity.maxHealth
    }

    @Ghost
    @SubscribeEvent
    private fun despawn(e: MythicMobDespawnEvent) {
        if (!MythicMobsCompatSupport.isLegacyMythicMobs) return
        MythicMobsCompatSupport.removeAttributes(e.entity.uniqueId)
    }

    @Ghost
    @SubscribeEvent
    private fun death(e: MythicMobDeathEvent) {
        if (!MythicMobsCompatSupport.isLegacyMythicMobs) return
        MythicMobsCompatSupport.removeAttributes(e.entity.uniqueId)
        val player = e.killer as? Player ?: return
        val drops = e.mob.type.config.getStringList("nodensDrops")
        val mob = e.mobType.internalName
        val location = BukkitAdapter.adapt(e.mob.location)
        MythicMobsCompatSupport.handleDrops(player, mob, drops, location)
    }
}
