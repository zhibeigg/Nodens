package org.gitee.nodens.module.formula

import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.util.consoleMessage
import org.gitee.nodens.util.files
import org.gitee.nodens.util.getBytes
import org.gitee.nodens.util.nodensEnvironmentNamespaces
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.console
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Configuration
import taboolib.module.kether.KetherScriptLoader
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptService
import java.util.concurrent.CompletableFuture

/**
 * 公式管理器：把 formulas/ 目录中按 id 定义的可复用 Kether 片段，在重载期**编译一次**为不可变
 * [Script]（= TabooLib Quest）并缓存，求值期只新建 [ScriptContext] 写入当前变量再执行。
 *
 * 与 [org.gitee.nodens.common.Handle] 同款预编译范式：相比旧实现每次走 `KetherShell.eval(字符串)`，
 * 这里在热点路径（lore 渲染、售价计算、跨插件怪物数值）上省去 ScriptOptions/VariableMap 分配、
 * 包裹串新建、O(n) 字符串哈希、ConcurrentHashMap 查找与一次冗余变量拷贝。
 */
object FormulaManager {

    /**
     * 已编译公式快照：重载时整体**原子替换**引用，绝不 clear 正在被读取的表。
     * 求值可能发生在非主线程（lore 渲染、售价计算、跨插件怪物数值），[Volatile] 保证
     * 读到的永远是一个一致快照，且没有 clear→重建之间的"空表窗口"。对齐 [org.gitee.nodens.common.Handle] 的安全语义。
     */
    @Volatile
    var formulaMap: Map<String, Formula> = emptyMap()
        private set

    private val ketherScriptLoader by lazy { KetherScriptLoader() }

    /**
     * 单条公式：[script] 为解析期编译好的不可变 AST，整生命周期复用。
     *
     * [Script] 不可变，可在主线程与并发间安全共享，前提是**每次执行新建 [ScriptContext]**——
     * 所有可变执行态（frame、@Sender、变量）都隔离在每次新建的 context 中。
     */
    class Formula(val id: String, val action: String, val script: Script) {

        /** 求值期：仅新建 ScriptContext + 写入本次变量；零再解析、零 ScriptOptions 分配。 */
        fun eval(sender: ProxyCommandSender?, vars: Map<String, Any?>): CompletableFuture<Any?> {
            return ScriptContext.create(script) {
                this.sender = sender ?: console()
                if (vars.isNotEmpty()) {
                    vars.forEach { (k, v) -> this[k] = v }
                }
            }.runActions()
        }
    }

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        reloadFormulas()
    }

    fun reloadFormulas() {
        // 先在本地表里编译重建，全部成功后再整体发布——读取方在重载期间始终看到旧快照，无空表窗口。
        val next = hashMapOf<String, Formula>()
        try {
            files("formulas", "example.yml") {
                val config = Configuration.loadFromFile(it)
                config.getKeys(false).forEach { id ->
                    val action = config.getString(id) ?: error("Missing $id Action")
                    // 单条 compile：单个公式脚本语法错误只跳过自身，不连累其余条目。
                    val script = compile(id, action)
                    if (script != null) {
                        next[id] = Formula(id, action, script)
                    } else {
                        warning("公式 $id 脚本编译失败，已跳过该条")
                    }
                }
            }
            formulaMap = next
            consoleMessage("&e┣&7Formula loaded &a√")
        } catch (e: Throwable) {
            // 重建失败则保留旧快照，避免线上公式整体失效。
            e.printStackTrace()
        }
    }

    private fun compile(id: String, action: String): Script? {
        return try {
            ketherScriptLoader.load(ScriptService, id, getBytes(action), nodensEnvironmentNamespaces)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
}
