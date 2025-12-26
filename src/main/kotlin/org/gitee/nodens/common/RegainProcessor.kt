package org.gitee.nodens.common

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.gitee.nodens.api.events.entity.NodensEntityRegainEvents
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.util.comparePriority

// reason NATURAL
class RegainProcessor(reason: String, val healer: LivingEntity, val passive: LivingEntity) {

    private var regainCache: Double? = null
    val reason = reason.uppercase()
    var scale = 1.0

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
        refresh()
    }

    fun getReduceSource(key: String): ReduceSource? {
        return reduceSources[key]
    }

    fun addReduceSource(key: String, attribute: IAttributeGroup.Number, reduce: Double) {
        reduceSources[key] = ReduceSource(key, attribute, reduce)
        refresh()
    }

    fun getFinalRegain(): Double {
        if (regainCache == null) {
            regainCache = Handle.runProcessor(this).coerceAtLeast(0.0)
        }
        return regainCache!!
    }

    fun refresh() {
        if (regainCache != null) {
            regainCache = null
        }
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
            val value = getFinalRegain()
            if (value == 0.0) return null
            Handle.doHeal(passive, value)?.apply {
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

    fun handle(vararg skipNumber: IAttributeGroup.Number) {
        handleHealer(*skipNumber)
        handlePassive(*skipNumber)
    }

    private fun handleHealer(vararg skipNumber: IAttributeGroup.Number) {
        healer.attributeMemory()?.mergedAllAttribute()?.toSortedMap { o1, o2 ->
            val priorityCompare = comparePriority(o1.config.handlePriority, o2.config.handlePriority)
            if (priorityCompare != 0) priorityCompare else o1.name.compareTo(o2.name)
        }?.forEach {
            if (it.key in skipNumber) return@forEach
            it.key.handleHealer(this, it.value)
        }
    }

    private fun handlePassive(vararg skipNumber: IAttributeGroup.Number) {
        passive.attributeMemory()?.mergedAllAttribute()?.toSortedMap { o1, o2 ->
            val priorityCompare = comparePriority(o1.config.handlePriority, o2.config.handlePriority)
            if (priorityCompare != 0) priorityCompare else o1.name.compareTo(o2.name)
        }?.forEach {
            if (it.key in skipNumber) return@forEach
            it.key.handlePassive(this, it.value)
        }
    }

    override fun toString(): String {
        return "RegainProcessor{reason: $reason, healer: $healer, passive: $passive, scale: $scale}"
    }
}