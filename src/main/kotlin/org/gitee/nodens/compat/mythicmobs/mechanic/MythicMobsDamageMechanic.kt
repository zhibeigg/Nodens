package org.gitee.nodens.compat.mythicmobs.mechanic

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.core.attribute.Damage
import org.gitee.nodens.util.NODENS_NAMESPACE
import taboolib.common.platform.Ghost

@Ghost
class MythicMobsDamageMechanic(line: String, mlc: MythicLineConfig) : SkillMechanic(line, mlc), ITargetedEntitySkill {

    private val type = mlc.getString(arrayOf("type", "t"))
    private val nPower = mlc.getDouble(arrayOf("power", "p"), 1.0)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): Boolean {
        return if (target.isPlayer) {
            val processor = DamageProcessor(type, data.caster.entity.bukkitEntity as? LivingEntity ?: return false, target.bukkitEntity as? LivingEntity ?: return false)
            processor.handleAttacker()
            processor.damageSources.forEach {
                when(it.key) {
                    "$NODENS_NAMESPACE${Damage.name}${Damage.Physics.name}" -> it.value.damage *= nPower
                    "$NODENS_NAMESPACE${Damage.name}${Damage.Magic.name}" -> it.value.damage *= nPower
                    "$NODENS_NAMESPACE${Damage.name}${Damage.Real.name}" -> it.value.damage *= nPower
                    "$NODENS_NAMESPACE${Damage.name}${Damage.Fire.name}" -> it.value.damage *= nPower
                }
            }
            processor.handleDefender()
            processor.callDamage()
            return true
        } else {
            false
        }
    }
}