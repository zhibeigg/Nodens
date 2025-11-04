package org.gitee.nodens.core.reload

import org.gitee.nodens.api.Nodens
import org.gitee.nodens.api.events.NodensPluginReloadEvent
import org.gitee.nodens.api.interfaces.IReloadAPI
import org.gitee.nodens.util.consoleMessage
import org.gitee.nodens.util.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.ClassMethod
import taboolib.library.reflex.ReflexClass
import taboolib.module.chat.colored

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
        val event = NodensPluginReloadEvent()
        if (event.call()) {
            consoleMessage("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Nodens.config.reload()
            val extensions = event.getFunctions()
            val weights = (methodList.map { it.weight } + extensions.map { it.weight }).distinct()
            weights.sorted().forEach { weight ->
                methodList.asSequence().filter { it.weight == weight }.forEach {
                    it.method.invoke(it.obj)
                }
                extensions.asSequence().filter { it.weight == weight }.forEach {
                    it.run()
                }
            }
            reloadMark ++
            consoleMessage("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    @Awake(LifeCycle.CONST)
    fun init() {
        PlatformFactory.registerAPI<IReloadAPI>(ReloadAPI)
    }
}