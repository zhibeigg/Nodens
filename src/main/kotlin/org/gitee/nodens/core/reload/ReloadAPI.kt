package org.gitee.nodens.core.reload

import org.gitee.nodens.api.Nodens
import org.gitee.nodens.api.events.NodensPluginReloadEvent
import org.gitee.nodens.api.interfaces.IReloadAPI
import org.gitee.nodens.common.Handle
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.module.item.ItemManager
import org.gitee.nodens.module.item.condition.ConditionManager
import org.gitee.nodens.module.item.group.GroupManager
import org.gitee.nodens.module.random.RandomManager
import org.gitee.nodens.util.consoleMessage
import org.gitee.nodens.util.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.ClassMethod
import taboolib.library.reflex.ReflexClass

@Awake
object ReloadAPI: IReloadAPI, ClassVisitor(3) {

    var reloadMark: Short = 0

    class ReloadFunction(val method: ClassMethod, val obj: Any, val weight: Int)

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.ENABLE
    }

    private val methodList by unsafeLazy { mutableListOf<ReloadFunction>() }

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
        fireReloadEventAndRun()
    }

    override fun reloadConfig() {
        Nodens.reloadConfig()
        reloadMark++
    }

    override fun reloadAttributes(updateEntities: Boolean) {
        AttributeManager.reloadAttributes()
        if (updateEntities) {
            EntityAttributeMemory.updateAllAttributeMemories()
        }
        reloadMark++
    }

    override fun reloadHandle() {
        Handle.reloadHandle()
        reloadMark++
    }

    override fun reloadItems() {
        ItemManager.reloadItems()
        reloadMark++
    }

    override fun reloadItemGroups() {
        GroupManager.reloadItemGroups()
        reloadMark++
    }

    override fun reloadConditions() {
        ConditionManager.reloadConditions()
        reloadMark++
    }

    override fun reloadRandoms() {
        RandomManager.reloadRandoms()
        reloadMark++
    }

    override fun reloadRegainTask() {
        EntityAttributeMemory.reloadRegainTask()
        reloadMark++
    }

    override fun reloadByWeight(weight: Int) {
        reloadByWeights(weight)
    }

    override fun reloadByWeights(vararg weights: Int) {
        if (weights.isEmpty()) {
            reload()
            return
        }
        fireReloadEventAndRun(weights.toSet())
    }

    private fun fireReloadEventAndRun(allowedWeights: Set<Int>? = null) {
        val event = NodensPluginReloadEvent()
        if (event.call()) {
            consoleMessage("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            runReloadFunctions(allowedWeights, event.getFunctions())
            reloadMark++
            consoleMessage("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    private fun runReloadFunctions(allowedWeights: Set<Int>?, extensions: List<NodensPluginReloadEvent.Func>) {
        val weights = (methodList.map { it.weight } + extensions.map { it.weight }).distinct()
        weights.sorted().forEach { weight ->
            if (allowedWeights != null && weight !in allowedWeights) return@forEach
            methodList.asSequence().filter { it.weight == weight }.forEach {
                it.method.invoke(it.obj)
            }
            extensions.asSequence().filter { it.weight == weight }.forEach {
                it.run()
            }
        }
    }

    @Awake(LifeCycle.CONST)
    fun init() {
        PlatformFactory.registerAPI<IReloadAPI>(ReloadAPI)
    }
}
