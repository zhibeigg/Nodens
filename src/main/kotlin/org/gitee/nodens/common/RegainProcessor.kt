package org.gitee.nodens.common

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.gitee.nodens.api.events.entity.NodensEntityRegainEvents
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory

// reason NATURAL
class RegainProcessor(val reason: String, val healer: LivingEntity, val passive: LivingEntity) {

    companion object {
        const val NATURAL_REASON = "NATURAL"
    }

    class PriorityRunnable(val priority: Int, val callback: (regain: Double) -> Unit)

    class RegainSource(override val key: String, override val attribute: IAttributeGroup.Number, val regain: Double): AbstractSource(regain) {

        override fun toString(): String {
            return "RegainSource{key: $key, attribute: $attribute, regain: $regain}"
        }
    }

    class ReduceSource(override val key: String, override val attribute: IAttributeGroup.Number, val reduce: Double): AbstractSource(reduce) {

        override fun toString(): String {
            return "ReduceSource{key: $key, attribute: $attribute, reduce: $reduce}"
        }
    }

    internal val regainSources = hashMapOf<String, RegainSource>()
    internal val reduceSources = hashMapOf<String, ReduceSource>()
    internal val runnableList = mutableListOf<PriorityRunnable>()

    fun getRegainSource(key: String): RegainSource? {
        return regainSources[key]
    }

    fun addRegainSource(key: String, attribute: IAttributeGroup.Number, regain: Double) {
        regainSources[key] = RegainSource(key, attribute, regain)
    }

    fun getReduceSource(key: String): ReduceSource? {
        return reduceSources[key]
    }

    fun addReduceSource(key: String, attribute: IAttributeGroup.Number, reduce: Double) {
        reduceSources[key] = ReduceSource(key, attribute, reduce)
    }

    fun getFinalRegain(): Double {
        return Handle.runProcessor(this).coerceAtLeast(0.0)
    }

    fun onRegain(priority: Int, callback: (damage: Double) -> Unit) {
        runnableList.add(PriorityRunnable(priority, callback))
    }

    /**
     * 执行恢复
     * @return [EntityRegainHealthEvent]实体恢复事件
     * */
    fun callRegain(): EntityRegainHealthEvent? {
        val event = NodensEntityRegainEvents.Pre(this)
        return if (event.call()) {
            Handle.doHeal(passive, getFinalRegain())?.apply {
                if (!isCancelled) {
                    callback(amount)
                    NodensEntityRegainEvents.Post(amount, this@RegainProcessor).call()
                }
            }
        } else {
            null
        }
    }

    fun callback(regain: Double) {
        runnableList.sortedBy { it.priority }.forEach {
            it.callback(regain)
        }
    }

    fun handle() {
        handleHealer()
        handlePassive()
    }

    private fun handleHealer() {
        healer.attributeMemory()?.mergedAllAttribute()?.toSortedMap { o1, o2 ->
            o1.config.handlePriority.compareTo(o2.config.handlePriority)
        }?.forEach {
            it.key.handleHealer(this, it.value)
        }
    }

    private fun handlePassive() {
        passive.attributeMemory()?.mergedAllAttribute()?.toSortedMap { o1, o2 ->
            o1.config.handlePriority.compareTo(o2.config.handlePriority)
        }?.forEach {
            it.key.handlePassive(this, it.value)
        }
    }

    override fun toString(): String {
        return "RegainProcessor{reason: $reason, healer: $healer, passive: $passive}"
    }
}