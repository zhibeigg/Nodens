package org.gitee.nodens.compat.mythicmobs

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.attribute.Damage
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.entityAttributeMemoriesMap
import org.gitee.nodens.module.item.drop.DropManager
import org.gitee.nodens.util.NODENS_NAMESPACE
import taboolib.common.util.random
import taboolib.common5.cbool
import taboolib.common5.cdouble
import taboolib.common5.cint
import java.util.UUID

object MythicMobsCompatSupport {

    private const val ATTRIBUTE_TAG = "Nodens@MythicMobs"

    private val DAMAGE_SOURCE_KEYS = setOf(
        "$NODENS_NAMESPACE${Damage.name}${Damage.MonsterAttack.name}",
        "$NODENS_NAMESPACE${Damage.name}${Damage.Physics.name}",
        "$NODENS_NAMESPACE${Damage.name}${Damage.Magic.name}",
        "$NODENS_NAMESPACE${Damage.name}${Damage.Real.name}",
        "$NODENS_NAMESPACE${Damage.name}${Damage.Fire.name}"
    )

    private val mythicMobsVersionRegex = Regex("^(\\d+)")

    val mythicMobsMajorVersion: Int?
        get() {
            val version = Bukkit.getPluginManager().getPlugin("MythicMobs")?.description?.version ?: return null
            return mythicMobsVersionRegex.find(version)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }

    val isMythicMobsLoaded: Boolean
        get() = Bukkit.getPluginManager().getPlugin("MythicMobs") != null

    val isMythicMobs5OrHigher: Boolean
        get() = mythicMobsMajorVersion?.let { it >= 5 } == true

    val isLegacyMythicMobs: Boolean
        get() = isMythicMobsLoaded && !isMythicMobs5OrHigher

    fun attachAttributes(entity: LivingEntity, attributeLines: List<String>, syncToBukkit: Boolean) {
        val attributes = Nodens.api().matchAttributes(attributeLines)
        entityAttributeMemoriesMap[entity.uniqueId] = EntityAttributeMemory(entity).apply {
            addAttribute(ATTRIBUTE_TAG, TempAttributeData(-1, attributes, true))
            if (syncToBukkit) {
                syncAttributeToBukkit()
            }
        }
    }

    fun removeAttributes(uniqueId: UUID) {
        entityAttributeMemoriesMap.remove(uniqueId)
    }

    fun handleDrops(player: Player, mob: String, drops: List<String>, location: Location) {
        drops.forEach { line ->
            val info = line.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (info.size < 2) return@forEach
            val drop = Drop(info)
            DropManager.tryDrop(player, mob, drop.item, drop.percent, location, drop.amount, drop.globalPrd)
        }
    }

    fun callDamage(type: String, power: Double, attacker: LivingEntity, defender: LivingEntity) {
        val processor = DamageProcessor(type, attacker, defender)
        processor.handleAttacker()
        processor.damageSources.forEach { (key, source) ->
            if (key in DAMAGE_SOURCE_KEYS) {
                source.damage *= power
            }
        }
        processor.handleDefender()
        processor.callDamage()
    }

    private class Drop(private val info: List<String>) {

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
