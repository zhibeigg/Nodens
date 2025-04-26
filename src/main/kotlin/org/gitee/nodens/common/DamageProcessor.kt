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
    class DamageSource(override val key: String, override val attribute: IAttributeGroup.Number, val damage: Double): AbstractSource(damage)
    class DefenceSource(override val key: String, override val attribute: IAttributeGroup.Number, val defence: Double): AbstractSource(defence)

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

    fun addDefenceSource(key: String, attribute: IAttributeGroup.Number, defence: Double) {
        defenceSources[key] = DefenceSource(key, attribute, defence)
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
        handleAttacker()
        handleDefender()
    }

    private fun handleAttacker() {
        attacker.attributeMemory()?.mergedAllAttribute()?.toSortedMap { o1, o2 ->
            o1.config.handlePriority.compareTo(o2.config.handlePriority)
        }?.forEach {
            it.key.handleAttacker(this, it.value)
        }
    }

    private fun handleDefender() {
        defender.attributeMemory()?.mergedAllAttribute()?.toSortedMap { o1, o2 ->
            o1.config.handlePriority.compareTo(o2.config.handlePriority)
        }?.forEach {
            it.key.handleDefender(this, it.value)
        }
    }
}