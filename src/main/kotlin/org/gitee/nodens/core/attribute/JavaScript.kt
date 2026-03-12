package org.gitee.nodens.core.attribute

import jdk.nashorn.api.scripting.ClassFilter
import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.AttributeManager.ATTRIBUTE_MATCHING_MAP
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.debug
import org.gitee.nodens.util.files
import org.apache.commons.lang3.tuple.Pair
import taboolib.common.platform.function.warning
import taboolib.common5.cdouble
import taboolib.common5.scriptEngine
import java.io.FileReader
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.script.*

object JavaScript: IAttributeGroup {

    /** 脚本执行超时时间(毫秒) */
    private const val SCRIPT_TIMEOUT_MS = 5000L

    /** 用于执行脚本的线程池 */
    private val scriptExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors().coerceAtLeast(2))

    private fun createSafeEngine(): ScriptEngine {
        return try {
            val factory = NashornScriptEngineFactory()
            factory.getScriptEngine(SafeClassFilter())
        } catch (e: Exception) {
            warning("无法创建安全脚本引擎(Nashorn 在 Java 15+ 已移除)，回退到默认引擎(无类过滤保护): ${e.message}")
            scriptEngine
        }
    }

    /**
     * 安全的类过滤器，只允许访问白名单中的类
     */
    private class SafeClassFilter : ClassFilter {
        private val allowedPackages = setOf(
            "org.gitee.nodens",
            "org.bukkit",
            "java.lang.String",
            "java.lang.Number",
            "java.lang.Integer",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Long",
            "java.lang.Boolean",
            "java.lang.Math",
            "java.util.List",
            "java.util.Map",
            "java.util.Set",
            "java.util.ArrayList",
            "java.util.HashMap",
            "java.util.HashSet"
        )

        private val blockedPackages = setOf(
            "java.io",
            "java.nio",
            "java.net",
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.System",
            "java.lang.reflect",
            "javax.script",
            "sun.",
            "com.sun."
        )

        override fun exposeToScripts(className: String): Boolean {
            // 阻止危险类
            if (blockedPackages.any { className.startsWith(it) }) {
                return false
            }
            // 允许白名单中的类或包
            return allowedPackages.any { className.startsWith(it) }
        }
    }

    /**
     * 带超时执行脚本函数
     * @param silent 为 true 时不打印错误日志（用于可选函数的探测调用）
     */
    private fun <T> invokeWithTimeout(invocable: Invocable, function: String, vararg args: Any?, silent: Boolean = false): T? {
        val future = scriptExecutor.submit(Callable {
            @Suppress("UNCHECKED_CAST")
            invocable.invokeFunction(function, *args) as T?
        })
        return try {
            future.get(SCRIPT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            if (!silent) warning("脚本函数 $function 执行超时 (>${SCRIPT_TIMEOUT_MS}ms)")
            null
        } catch (e: Exception) {
            if (!silent) warning("脚本函数 $function 执行错误: ${e.message}")
            null
        }
    }

    override val name: String = "JavaScript"

    override val numbers = hashMapOf<String, JsAttribute>()

    /**
     * 标准属性值计算（与 AbstractNumber.getValue 一致）
     */
    private fun getValueFromMap(valueMap: Map<DigitalParser.Type, DoubleArray>): Pair<Double, Double> {
        var value: Pair<Double, Double> = Pair.of(0.0, 0.0)
        val count = valueMap[DigitalParser.Type.COUNT]
        if (count != null) {
            value = if (count.size == 2) {
                Pair.of(count[0], count[1])
            } else {
                Pair.of(count[0], count[0])
            }
        }
        val percent = valueMap[DigitalParser.Type.PERCENT]
        if (percent != null) {
            value = Pair.of(value.left * (1 + percent[0]), value.right * (1 + percent[0]))
        }
        return value
    }

    private fun createBindings(config: AttributeConfig) = SimpleBindings(mapOf(
        "keys" to config.keys,
        "valueType" to config.valueType.name,
        "combatPower" to config.combatPower,
        "priority" to config.syncPriority
    ))

    class JsAttribute(override val name: String, val compile: CompiledScript, val engine: ScriptEngine): IAttributeGroup.Number {

        override val group: IAttributeGroup
            get() = JavaScript

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(JavaScript.name, name)

        private val invocable: Invocable
            get() = engine as Invocable

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            invokeWithTimeout<Unit>(invocable, "sync", entitySyncProfile, valueMap)
        }

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            invokeWithTimeout<Unit>(invocable, "handleAttacker", damageProcessor, valueMap)
        }

        override fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            invokeWithTimeout<Unit>(invocable, "handleDefender", damageProcessor, valueMap)
        }

        override fun handleHealer(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            invokeWithTimeout<Unit>(invocable, "handleHealer", regainProcessor, valueMap)
        }

        override fun handlePassive(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            invokeWithTimeout<Unit>(invocable, "handlePassive", regainProcessor, valueMap)
        }

        override fun combatPower(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
            return invokeWithTimeout<Any>(invocable, "combatPower", valueMap)?.cdouble ?: 0.0
        }

        override fun getFinalValue(entity: LivingEntity, valueMap: Map<DigitalParser.Type, DoubleArray>): IAttributeGroup.Number.FinalValue {
            // 尝试调用脚本中的 getFinalValue 函数（未定义时静默返回 null）
            val scriptResult = invokeWithTimeout<Any>(invocable, "getFinalValue", entity, valueMap, silent = true)
            if (scriptResult != null) {
                // 脚本返回了结果，尝试解析
                if (scriptResult is IAttributeGroup.Number.FinalValue) {
                    return scriptResult
                }
                // 脚本返回了数值，包装为 SINGLE 类型
                val numValue = scriptResult.cdouble
                return object : IAttributeGroup.Number.FinalValue {
                    override val type = config.valueType
                    override val value: Double? = numValue
                    override val rangeValue: Pair<Double, Double>? = null
                }
            }
            // 回退到标准计算逻辑（与 AbstractNumber.getFinalValue 一致）
            val value = getValueFromMap(valueMap)
            return object : IAttributeGroup.Number.FinalValue {
                override val type = config.valueType
                override val value: Double? = when (config.valueType) {
                    IAttributeGroup.Number.ValueType.RANGE -> null
                    IAttributeGroup.Number.ValueType.SINGLE -> value.left
                }
                override val rangeValue: Pair<Double, Double>? = when (config.valueType) {
                    IAttributeGroup.Number.ValueType.RANGE -> value
                    IAttributeGroup.Number.ValueType.SINGLE -> null
                }
            }
        }

        override fun toString(): String {
            return "JavaScriptAttributeNumber{name: ${name}}"
        }
    }

    internal fun reload() {
        numbers.clear()
        files("js", "Fire.js") { file ->
            try {
                FileReader(file).use { reader ->
                    // 为每个脚本创建独立的引擎
                    val engine = createSafeEngine()
                    val compile = (engine as? Compilable)?.compile(reader) ?: run {
                        warning("无法编译脚本: ${file.name} - 引擎不支持编译")
                        return@files
                    }

                    val attributeName = file.nameWithoutExtension
                    val attribute = JsAttribute(attributeName, compile, engine)
                    numbers[attribute.name] = attribute

                    // 先将变量绑定到引擎全局作用域
                    val bindings = createBindings(attribute.config)
                    bindings.forEach { (key, value) ->
                        engine.put(key, value)
                    }
                    // 然后执行脚本，使函数定义在全局作用域中
                    compile.eval()

                    attribute.config.keys.forEach { key ->
                        ATTRIBUTE_MATCHING_MAP.put(key, attribute)
                        debug("&6│ &7│ &7└ &dJS &8» &a$key &a✔")
                    }
                }
            } catch (e: Exception) {
                warning("加载属性脚本 ${file.name} 时出错: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}