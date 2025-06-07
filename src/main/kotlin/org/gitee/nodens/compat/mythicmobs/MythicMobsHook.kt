package org.gitee.nodens.compat.mythicmobs

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDespawnEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.compat.mythicmobs.mechanic.MythicMobsDamageMechanic
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.entityAttributeMemoriesMap
import org.gitee.nodens.module.item.drop.DropManager
import org.gitee.nodens.util.MythicMobsPlugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.random
import taboolib.common5.cbool
import taboolib.common5.cdouble
import taboolib.common5.cint

object MythicMobsHook {

    private const val ATTRIBUTE_TAG = "Nodens@MythicMobs"

    @Awake(LifeCycle.ENABLE)
    private fun loadActive() {
        if (!MythicMobsPlugin.isEnabled) return
        MythicMobs.inst().mobManager.activeMobs.forEach {
            val attributes = Nodens.api().matchAttributes(it.type.config.getStringList("Nodens"))
            entityAttributeMemoriesMap[it.uniqueId] = EntityAttributeMemory(it.entity.bukkitEntity as? LivingEntity ?: return).apply {
                addAttribute(ATTRIBUTE_TAG, TempAttributeData(-1, attributes, true))
            }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun registerMechanic(event: MythicMechanicLoadEvent) {
        when (event.mechanicName.uppercase()) {
            "NO-DAMAGE" -> { event.register(MythicMobsDamageMechanic(event.container.configLine, event.config)) }
        }
    }

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
        val player = e.killer as? Player ?: return
        val drops = e.mob.type.config.getStringList("nodensDrops")
        val mob = e.mobType.internalName
        val location = BukkitAdapter.adapt(e.mob.location)
        drops.forEach {
            val drop = Drop(it.split(" "))
            DropManager.tryDrop(player, mob, drop.item, drop.percent, location, drop.amount, drop.globalPrd)
        }
    }

    class Drop(val info: List<String>) {

        val item: String = info[0]

        val percent: Double = info[1].cdouble

        val globalPrd: Boolean = info.getOrElse(2) { "false" }.cbool

        val amount: Int
            get() {
                val list = info.getOrElse(3) { "1" }.split("-")
                return if (list.size == 1) {
                    list[0].cint
                } else {
                    random(list[0].cint, list[1].cint)
                }
            }
    }
}