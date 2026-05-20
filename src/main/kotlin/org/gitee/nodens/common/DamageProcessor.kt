package org.gitee.nodens.common

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.gitee.nodens.api.DamageFormulaProvider
import org.gitee.nodens.api.events.entity.NodensEntityDamageEvents
import org.gitee.nodens.api.result.RegisterResult
import org.gitee.nodens.common.Handle.doDamage
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.attribute.Crit
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.util.NODENS_NAMESPACE
import java.util.concurrent.ConcurrentHashMap

/**
 * @param damageType 攻击类型，一般来自[org.gitee.nodens.core.attribute.Damage]中的[IAttributeGroup.Number.name]
 * @param attacker 攻击者
 * @param defender 防御者
 * */
class DamageProcessor(damageType: String, val attacker: LivingEntity, val defender: LivingEntity) {

    companion object {

        private data class RegisteredDamageFormulaProvider(
            val id: String,
            val priority: Int,
            val provider: DamageFormulaProvider,
        )

        private val formulaProviders = ConcurrentHashMap<String, RegisteredDamageFormulaProvider>()

        fun registerDamageFormulaProvider(id: String, priority: Int, provider: DamageFormulaProvider): RegisterResult {
            return runCatching {
                require(id.isNotBlank()) { "伤害公式提供者 ID 不能为空" }
                val replaced = formulaProviders.put(id, RegisteredDamageFormulaProvider(id, priority, provider))
                RegisterResult.success(if (replaced == null) "伤害公式提供者 $id 注册成功" else "伤害公式提供者 $id 已替换")
            }.getOrElse {
                RegisterResult.failure("伤害公式提供者 $id 注册失败: ${it.message ?: it.javaClass.simpleName}", it)
            }
        }

        fun unregisterDamageFormulaProvider(id: String): RegisterResult {
            return runCatching {
                val removed = formulaProviders.remove(id)
                if (removed == null) {
                    RegisterResult.failure("伤害公式提供者 $id 不存在")
                } else {
                    RegisterResult.success("伤害公式提供者 $id 已注销")
                }
            }.getOrElse {
                RegisterResult.failure("伤害公式提供者 $id 注销失败: ${it.message ?: it.javaClass.simpleName}", it)
            }
        }

        fun getDamageFormulaProviders(): Map<String, DamageFormulaProvider> {
            return formulaProviders.mapValues { it.value.provider }
        }

        private fun calculateWithProvider(processor: DamageProcessor): Double? {
            return formulaProviders.values
                .sortedWith(compareBy<RegisteredDamageFormulaProvider> { it.priority }.thenBy { it.id })
                .firstNotNullOfOrNull { registered ->
                    runCatching { registered.provider.calculate(processor) }.getOrNull()
                }
        }
    }

    private var damageCache: Double? = null

    val damageType = damageType.uppercase()
    var scale = 1.0

    var crit: Boolean = false
        internal set

    fun setCrit(crit: Boolean) {
        if (this.crit != crit) {
            if (crit) {
                val memory = attacker.attributeMemory() ?: return
                Crit.Addon.handleAttacker(this, memory.mergedAttribute(Crit.Addon))
                Crit.CritAddonResistance.handleDefender(this, memory.mergedAttribute(Crit.CritAddonResistance))
                this.crit = true
            } else {
                damageSources.remove("$NODENS_NAMESPACE${Crit.name}${Crit.Addon.name}")
                defenceSources.remove("$NODENS_NAMESPACE${Crit.name}${Crit.CritAddonResistance.name}")
                this.crit = false
            }
            refresh()
        }
    }

    class PriorityRunnable(val priority: Int, val callback: (damage: Double) -> Unit)

    class DamageSource(override val key: String, override val attribute: IAttributeGroup.Number, var damage: Double): AbstractSource(damage) {

        override fun toString(): String {
            return "ReduceSource{key: $key, attribute: $attribute, damage: $damage}"
        }
    }

    class DefenceSource(override val key: String, override val attribute: IAttributeGroup.Number, var defence: Double): AbstractSource(defence) {

        override fun toString(): String {
            return "ReduceSource{key: $key, attribute: $attribute, defence: $defence}"
        }
    }

    // 预分配 HashMap 容量，减少扩容开销
    internal val damageSources = HashMap<String, DamageSource>(16)
    internal val defenceSources = HashMap<String, DefenceSource>(16)
    internal val runnableList = ArrayList<PriorityRunnable>(4)

    fun getDamageSource(key: String): DamageSource? {
        return damageSources[key]
    }

    fun addDamageSource(key: String, attribute: IAttributeGroup.Number, damage: Double) {
        damageSources[key] = DamageSource(key, attribute, damage)
        refresh()
    }

    fun getDefenceSource(key: String): DefenceSource? {
        return defenceSources[key]
    }

    fun addDefenceSource(key: String, attribute: IAttributeGroup.Number, defence: Double) {
        defenceSources[key] = DefenceSource(key, attribute, defence)
        refresh()
    }

    fun getFinalDamage(): Double {
        if (damageCache == null) {
            damageCache = (calculateWithProvider(this) ?: Handle.runProcessor(this)).coerceAtLeast(0.0)
        }
        return damageCache!!
    }

    fun refresh() {
        if (damageCache != null) {
            damageCache = null
        }
    }

    fun onDamage(priority: Int, callback: (damage: Double) -> Unit) {
        runnableList.add(PriorityRunnable(priority, callback))
    }

    /**
     * 执行攻击
     * @return [EntityDamageByEntityEvent]实体攻击事件
     * */
    fun callDamage(): EntityDamageByEntityEvent? {
        val event = NodensEntityDamageEvents.Pre(this)
        return if (event.call()) {
            val value = getFinalDamage()
            if (value == 0.0) return null
            doDamage(attacker, defender, EntityDamageEvent.DamageCause.CUSTOM, value)?.apply {
                if (!isCancelled) {
                    callback(finalDamage)
                    NodensEntityDamageEvents.Post(finalDamage, this@DamageProcessor).call()
                }
            }
        } else {
            null
        }
    }

    fun callback(damage: Double) {
        runnableList.sortedBy { it.priority }.forEach {
            it.callback(damage)
        }
    }

    fun handle(vararg skipNumber: IAttributeGroup.Number) {
        handleAttacker(*skipNumber)
        handleDefender(*skipNumber)
    }

    fun handleAttacker(vararg skipNumber: IAttributeGroup.Number) {
        attacker.attributeMemory()?.mergedAllAttribute()
            ?.toSortedMap(AttributeManager.ATTRIBUTE_COMPARATOR)
            ?.forEach {
                if (it.key in skipNumber) return@forEach
                it.key.handleAttacker(this, it.value)
            }
    }

    fun handleDefender(vararg skipNumber: IAttributeGroup.Number) {
        defender.attributeMemory()?.mergedAllAttribute()
            ?.toSortedMap(AttributeManager.ATTRIBUTE_COMPARATOR)
            ?.forEach {
                if (it.key in skipNumber) return@forEach
                it.key.handleDefender(this, it.value)
            }
    }

    override fun toString(): String {
        return "DamageProcessor{damageType: $damageType, crit: $crit, attacker: $attacker, defender: $defender, scale: $scale}"
    }
}