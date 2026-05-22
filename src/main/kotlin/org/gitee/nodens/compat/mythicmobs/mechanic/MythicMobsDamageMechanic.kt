package org.gitee.nodens.compat.mythicmobs.mechanic

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.compat.mythicmobs.MythicMobsCompatSupport

class MythicMobsDamageMechanic(line: String, mlc: MythicLineConfig) : SkillMechanic(line, mlc), ITargetedEntitySkill {

    private val type = mlc.getString(arrayOf("type", "t"))
    private val nPower = mlc.getDouble(arrayOf("power", "p"), 1.0)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): Boolean {
        val attacker = data.caster.entity.bukkitEntity as? LivingEntity ?: return false
        val defender = target.bukkitEntity as? LivingEntity ?: return false

        data.caster.isUsingDamageSkill = true
        try {
            MythicMobsCompatSupport.callDamage(type, nPower, attacker, defender)
        } finally {
            data.caster.isUsingDamageSkill = false
        }
        return true
    }
}
