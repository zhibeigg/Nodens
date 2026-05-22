package org.gitee.nodens.compat.mythicmobs.v5

import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent
import io.lumine.mythic.core.skills.SkillExecutor
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.compat.mythicmobs.MythicMobsCompatSupport
import org.gitee.nodens.compat.mythicmobs.v5.mechanic.MythicMobs5DamageMechanic
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import java.io.File

object MythicMobs5Hook {

    @Ghost
    @Awake(LifeCycle.ENABLE)
    private fun loadActive() {
        if (!MythicMobsCompatSupport.isMythicMobs5OrHigher) return
        MythicBukkit.inst().getMobManager().getActiveMobs().forEach { activeMob ->
            val entity = activeMob.getEntity().getBukkitEntity() as? LivingEntity ?: return@forEach
            val attributes = activeMob.getType().getConfig().getStringList("Nodens")
            MythicMobsCompatSupport.attachAttributes(entity, attributes, false)
        }
    }

    @Ghost
    @SubscribeEvent
    private fun registerMechanic(event: MythicMechanicLoadEvent) {
        if (!MythicMobsCompatSupport.isMythicMobs5OrHigher) return
        if (event.getMechanicName().uppercase() != "NO-DAMAGE") return
        val skillExecutor = MythicBukkit.inst().getSkillManager() as? SkillExecutor ?: return
        val container = event.getContainer()
        event.register(
            MythicMobs5DamageMechanic(
                skillExecutor,
                File(container.getFilePath()),
                container.getConfigLine(),
                event.getConfig()
            )
        )
    }

    @Ghost
    @SubscribeEvent
    private fun spawn(event: MythicMobSpawnEvent) {
        if (!MythicMobsCompatSupport.isMythicMobs5OrHigher) return
        val entity = event.getEntity() as? LivingEntity ?: return
        MythicMobsCompatSupport.attachAttributes(entity, event.getMobType().getConfig().getStringList("Nodens"), true)
        val mythicEntity = event.getMob().getEntity()
        mythicEntity.setHealth(mythicEntity.getMaxHealth())
    }

    @Ghost
    @SubscribeEvent
    private fun despawn(event: MythicMobDespawnEvent) {
        if (!MythicMobsCompatSupport.isMythicMobs5OrHigher) return
        MythicMobsCompatSupport.removeAttributes(event.getEntity().getUniqueId())
    }

    @Ghost
    @SubscribeEvent
    private fun death(event: MythicMobDeathEvent) {
        if (!MythicMobsCompatSupport.isMythicMobs5OrHigher) return
        val entity = event.getEntity()
        MythicMobsCompatSupport.removeAttributes(entity.getUniqueId())
        val player = event.getKiller() as? Player ?: return
        val mobType = event.getMobType()
        MythicMobsCompatSupport.handleDrops(
            player,
            mobType.getInternalName(),
            mobType.getConfig().getStringList("nodensDrops"),
            entity.getLocation()
        )
    }
}
