package org.gitee.nodens.core.reload

import org.gitee.nodens.api.Nodens
import org.gitee.nodens.api.events.NodensPluginReloadEvent
import org.gitee.nodens.api.interfaces.IReloadAPI
import org.gitee.nodens.api.result.RegisterResult
import org.gitee.nodens.api.result.ReloadResult
import org.gitee.nodens.common.Handle
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.module.item.ItemManager
import org.gitee.nodens.module.item.condition.ConditionManager
import org.gitee.nodens.module.item.group.GroupManager
import org.gitee.nodens.module.formula.FormulaManager
import org.gitee.nodens.util.consoleMessage
import org.gitee.nodens.util.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.ClassMethod
import taboolib.library.reflex.ReflexClass
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Awake
object ReloadAPI: IReloadAPI, ClassVisitor(3) {

    var reloadMark: Short = 0

    class ReloadFunction(val method: ClassMethod, val obj: Any, val weight: Int)

    data class ReloadHook(val owner: String, val weight: Int, val function: Runnable)

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.ENABLE
    }

    private val methodList by unsafeLazy { mutableListOf<ReloadFunction>() }
    private val reloadHooks = ConcurrentHashMap<String, CopyOnWriteArrayList<ReloadHook>>()

    override fun visit(method: ClassMethod, owner: ReflexClass) {
        if (method.isAnnotationPresent(Reload::class.java)) {
            methodList += ReloadFunction(
                method,
                owner.getInstance() ?: return,
                method.getAnnotation(Reload::class.java).enum("weight")
            )
            debug("&e┣&7Reload loaded &e${method.owner.name}/${method.name} &a√")
        }
    }

    override fun reload() {
        reloadResult()
    }

    override fun reloadResult(): ReloadResult {
        return fireReloadEventAndRun()
    }

    override fun reloadConfig() {
        reloadConfigResult()
    }

    override fun reloadConfigResult(): ReloadResult {
        return runReloadAction("config.yml 重载") {
            Nodens.reloadConfig()
        }
    }

    override fun reloadAttributes(updateEntities: Boolean) {
        reloadAttributesResult(updateEntities)
    }

    override fun reloadAttributesResult(updateEntities: Boolean): ReloadResult {
        return runReloadAction("属性系统重载") {
            AttributeManager.reloadAttributes()
            if (updateEntities) {
                EntityAttributeMemory.updateAllAttributeMemories()
            }
        }
    }

    override fun reloadHandle() {
        reloadHandleResult()
    }

    override fun reloadHandleResult(): ReloadResult {
        return runReloadAction("handle.yml 重载") {
            Handle.reloadHandle()
        }
    }

    override fun reloadItems() {
        reloadItemsResult()
    }

    override fun reloadItemsResult(): ReloadResult {
        return runReloadAction("物品配置重载") {
            ItemManager.reloadItems()
        }
    }

    override fun reloadItemGroups() {
        reloadItemGroupsResult()
    }

    override fun reloadItemGroupsResult(): ReloadResult {
        return runReloadAction("物品组配置重载") {
            GroupManager.reloadItemGroups()
        }
    }

    override fun reloadConditions() {
        reloadConditionsResult()
    }

    override fun reloadConditionsResult(): ReloadResult {
        return runReloadAction("条件匹配表重载") {
            ConditionManager.reloadConditions()
        }
    }

    override fun reloadFormulas() {
        reloadFormulasResult()
    }

    override fun reloadFormulasResult(): ReloadResult {
        return runReloadAction("公式配置重载") {
            FormulaManager.reloadFormulas()
        }
    }

    override fun reloadRegainTask() {
        reloadRegainTaskResult()
    }

    override fun reloadRegainTaskResult(): ReloadResult {
        return runReloadAction("自然恢复任务重载") {
            EntityAttributeMemory.reloadRegainTask()
        }
    }

    override fun reloadByWeight(weight: Int) {
        reloadByWeightResult(weight)
    }

    override fun reloadByWeightResult(weight: Int): ReloadResult {
        return reloadByWeightsResult(weight)
    }

    override fun reloadByWeights(vararg weights: Int) {
        reloadByWeightsResult(*weights)
    }

    override fun reloadByWeightsResult(vararg weights: Int): ReloadResult {
        if (weights.isEmpty()) {
            return reloadResult()
        }
        return fireReloadEventAndRun(weights.toSet())
    }

    override fun registerReloadHook(owner: String, weight: Int, function: Runnable): RegisterResult {
        return runCatching {
            require(owner.isNotBlank()) { "Reload Hook owner 不能为空" }
            reloadHooks.computeIfAbsent(owner) { CopyOnWriteArrayList() } += ReloadHook(owner, weight, function)
            RegisterResult.success("Reload Hook 注册成功 owner=$owner weight=$weight")
        }.getOrElse {
            RegisterResult.failure("Reload Hook 注册失败 owner=$owner: ${it.message ?: it.javaClass.simpleName}", it)
        }
    }

    override fun unregisterReloadHooks(owner: String): RegisterResult {
        return runCatching {
            val removed = reloadHooks.remove(owner)?.size ?: 0
            if (removed == 0) {
                RegisterResult.failure("Reload Hook owner=$owner 不存在")
            } else {
                RegisterResult.success("Reload Hook 注销成功 owner=$owner count=$removed")
            }
        }.getOrElse {
            RegisterResult.failure("Reload Hook 注销失败 owner=$owner: ${it.message ?: it.javaClass.simpleName}", it)
        }
    }

    private fun runReloadAction(name: String, action: () -> Unit): ReloadResult {
        return runCatching {
            action()
            reloadMark++
            ReloadResult.success("$name 完成")
        }.getOrElse {
            ReloadResult.failure("$name 失败: ${it.message ?: it.javaClass.simpleName}", it)
        }
    }

    private fun fireReloadEventAndRun(allowedWeights: Set<Int>? = null): ReloadResult {
        return runCatching {
            val event = NodensPluginReloadEvent()
            if (!event.call()) {
                return ReloadResult.failure("Nodens 重载事件已取消")
            }
            consoleMessage("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            runReloadFunctions(allowedWeights, event.getFunctions(), reloadHooks.values.flatten())
            reloadMark++
            consoleMessage("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            ReloadResult.success(if (allowedWeights == null) "Nodens 全部模块重载完成" else "Nodens 指定权重重载完成 weights=${allowedWeights.joinToString()}")
        }.getOrElse {
            ReloadResult.failure("Nodens 重载失败: ${it.message ?: it.javaClass.simpleName}", it)
        }
    }

    private fun runReloadFunctions(
        allowedWeights: Set<Int>?,
        extensions: List<NodensPluginReloadEvent.Func>,
        hooks: List<ReloadHook>,
    ) {
        val weights = (methodList.map { it.weight } + extensions.map { it.weight } + hooks.map { it.weight }).distinct()
        weights.sorted().forEach { weight ->
            if (allowedWeights != null && weight !in allowedWeights) return@forEach
            methodList.asSequence().filter { it.weight == weight }.forEach {
                it.method.invoke(it.obj)
            }
            extensions.asSequence().filter { it.weight == weight }.forEach {
                it.run()
            }
            hooks.asSequence().filter { it.weight == weight }.forEach {
                it.function.run()
            }
        }
    }

    @Awake(LifeCycle.CONST)
    fun init() {
        PlatformFactory.registerAPI<IReloadAPI>(ReloadAPI)
    }
}
