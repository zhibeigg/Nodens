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

class MythicMobsDamageMechanic(line: String, mlc: MythicLineConfig) : SkillMechanic(line, mlc), ITargetedEntitySkill {

    companion object {
        /** 预计算的伤害源 key 集合，避免每次调用都拼接字符串 */
        private val DAMAGE_SOURCE_KEYS = setOf(
            "$NODENS_NAMESPACE${Damage.name}${Damage.MonsterAttack.name}",
            "$NODENS_NAMESPACE${Damage.name}${Damage.Physics.name}",
            "$NODENS_NAMESPACE${Damage.name}${Damage.Magic.name}",
            "$NODENS_NAMESPACE${Damage.name}${Damage.Real.name}",
            "$NODENS_NAMESPACE${Damage.name}${Damage.Fire.name}"
        )
    }

    private val type = mlc.getString(arrayOf("type", "t"))
    private val nPower = mlc.getDouble(arrayOf("power", "p"), 1.0)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): Boolean {
        val attacker = data.caster.entity.bukkitEntity as? LivingEntity ?: return false
        val defender = target.bukkitEntity as? LivingEntity ?: return false

        val processor = DamageProcessor(type, attacker, defender)
        processor.handleAttacker()

        // 使用预计算的 key 集合进行匹配，避免重复字符串拼接
        processor.damageSources.forEach { (key, source) ->
            if (key in DAMAGE_SOURCE_KEYS) {
                source.damage *= nPower
            }
        }

        processor.handleDefender()
        data.caster.isUsingDamageSkill = true
        try {
            processor.callDamage()
        } finally {
            data.caster.isUsingDamageSkill = false
        }
        return true
    }
}