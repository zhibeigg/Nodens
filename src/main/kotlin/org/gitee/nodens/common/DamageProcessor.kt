package org.gitee.nodens.common

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.gitee.nodens.common.Handle.doDamage
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.attribute.Defence
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory

/**
 * @param damageType 攻击类型，一般来自[org.gitee.nodens.core.attribute.Damage]中的[IAttributeGroup.Number.name]
 * @param attacker 攻击者
 * @param defender 防御者
 * */
class DamageProcessor(damageType: String, val attacker: LivingEntity, val defender: LivingEntity) {

    val damageType = damageType.uppercase()
    var crit = false

    class PriorityRunnable(val priority: Int, val callback: (damage: Double) -> Unit)
    class DamageSource(val key: String, val attribute: IAttributeGroup.Number, val damage: Double)
    class DefenceSource(val key: String, val attribute: IAttributeGroup.Number, val defence: Double)

    internal val damageSources = hashMapOf<String, DamageSource>()
    internal val defenceSources = hashMapOf<String, DefenceSource>()
    internal val runnableList = mutableListOf<PriorityRunnable>()

    fun getDamageSource(key: String): DamageSource? {
        return damageSources[key]
    }

    fun addDamageSource(key: String, attribute: IAttributeGroup.Number, damage: Double) {
        damageSources[key] = DamageSource(key, attribute, damage)
    }

    fun getDefenceSource(key: String): DefenceSource? {
        return defenceSources[key]
    }

    fun addDefenceSource(key: String, attribute: IAttributeGroup.Number, defence: Double): Boolean {
        defenceSources[key] = when(attribute) {
            Defence.Physics -> if (damageType == "PHYSICS") DefenceSource(key, attribute, defence) else return false
            Defence.Magic -> if (damageType == "MAGIC") DefenceSource(key, attribute, defence) else return false
            else -> {
                if(attribute.name.uppercase() == damageType) {
                    DefenceSource(key, attribute, defence)
                } else {
                    return false
                }
            }
        }
        return true
    }

    fun getFinalDamage(): Double {
        return Handle.runProcessor(this).coerceAtLeast(0.0)
    }

    fun onDamage(priority: Int, callback: (damage: Double) -> Unit) {
        runnableList.add(PriorityRunnable(priority, callback))
    }

    /**
     * 执行攻击
     * @return [EntityDamageByEntityEvent]实体攻击事件
     * */
    fun callDamage(): EntityDamageByEntityEvent? {
        return doDamage(attacker, defender, EntityDamageEvent.DamageCause.CUSTOM, getFinalDamage())?.apply {
            if (!isCancelled) {
                callback(finalDamage)
            }
        }
    }

    fun callback(damage: Double) {
        runnableList.sortedBy { it.priority }.forEach {
            it.callback(damage)
        }
    }

    fun handle() {
        handle(attacker)
        handle(defender)
    }

    private fun handle(entity: LivingEntity) {
        entity.attributeMemory()?.mergedAllAttribute()?.toSortedMap { o1, o2 ->
            o1.config.handlePriority.compareTo(o2.config.handlePriority)
        }?.forEach {
            it.key.handleAttacker(this, it.value)
        }
    }
}