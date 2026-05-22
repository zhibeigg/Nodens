package org.gitee.nodens.compat.mythicmobs.v5.mechanic

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.core.skills.SkillExecutor
import io.lumine.mythic.core.skills.SkillMechanic
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.compat.mythicmobs.MythicMobsCompatSupport
import java.io.File

class MythicMobs5DamageMechanic(
    manager: SkillExecutor,
    file: File,
    line: String,
    mlc: MythicLineConfig
) : SkillMechanic(manager, file, line, mlc), ITargetedEntitySkill {

    private val type = mlc.getString(arrayOf("type", "t"))
    private val nPower = mlc.getDouble(arrayOf("power", "p"), 1.0)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): SkillResult {
        val caster = data.getCaster()
        val attacker = caster.getEntity().getBukkitEntity() as? LivingEntity ?: return SkillResult.INVALID_TARGET
        val defender = target.getBukkitEntity() as? LivingEntity ?: return SkillResult.INVALID_TARGET

        caster.setUsingDamageSkill(true)
        try {
            MythicMobsCompatSupport.callDamage(type, nPower, attacker, defender)
        } finally {
            caster.setUsingDamageSkill(false)
        }
        return SkillResult.SUCCESS
    }
}
