package org.gitee.nodens.module.item

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Variable 序列化适配器接口
 *
 * 外部插件实现此接口来提供自定义 Variable 的序列化/反序列化逻辑，
 * 无需依赖 kotlinx.serialization 的类型。
 *
 * @param T Variable 子类的类型
 */
interface VariableAdapter<T : Variable<*>> {

    /**
     * 序列化名称，用于 JSON 中的类型标识
     * 例如: "SpiritItemVariable"
     */
    val serialName: String

    /**
     * Variable 子类的 Class
     */
    val variableClass: Class<T>

    /**
     * 将 Variable 序列化为 JSON 字符串
     *
     * @param variable 要序列化的 Variable 实例
     * @return JSON 字符串表示
     */
    fun serialize(variable: T): String

    /**
     * 从 JSON 字符串反序列化为 Variable
     *
     * @param json JSON 字符串
     * @return 反序列化后的 Variable 实例
     */
    fun deserialize(json: String): T

    /**
     * 检查一个对象是否可以转换为此 Variable 类型
     *
     * @param value 要检查的对象
     * @return 如果可以转换返回 true
     */
    fun canConvert(value: Any): Boolean

    /**
     * 将对象转换为 Variable
     *
     * 注意: 在调用此方法前应先调用 canConvert 检查
     *
     * @param value 要转换的对象
     * @return 转换后的 Variable 实例
     */
    fun convert(value: Any): T
}

/**
 * Variable 注册中心
 *
 * 用于注册外部模块的 Variable 子类，使其支持多态序列化和 toVariable 转换。
 *
 * 使用示例：
 * ```kotlin
 * // 1. 实现 VariableAdapter
 * class SpiritItemVariableAdapter : VariableAdapter<SpiritItemVariable> {
 *     override val serialName = "SpiritItemVariable"
 *     override val variableClass = SpiritItemVariable::class.java
 *
 *     override fun serialize(variable: SpiritItemVariable): String {
 *         // 使用你自己的 JSON 库序列化
 *         return yourJson.encodeToString(variable)
 *     }
 *
 *     override fun deserialize(json: String): SpiritItemVariable {
 *         return yourJson.decodeFromString(json)
 *     }
 *
 *     override fun canConvert(value: Any): Boolean {
 *         return value is SpiritItem
 *     }
 *
 *     override fun convert(value: Any): SpiritItemVariable {
 *         return SpiritItemVariable(value as SpiritItem)
 *     }
 * }
 *
 * // 2. 注册
 * VariableRegistry.register(SpiritItemVariableAdapter())
 * VariableRegistry.rebuild()
 * ```
 *
 * 注意：
 * - 注册必须在使用序列化之前完成
 * - 建议在 TabooLib 的 LOAD 或 ENABLE 生命周期中注册
 * - 每次注册新的子类后，需要调用 rebuild() 方法
 */
object VariableRegistry {

    private val adapters = mutableMapOf<String, VariableAdapter<*>>()

    /**
     * 当前配置好的 Json 实例，支持已注册的 Variable 多态序列化
     */
    @Volatile
    @JvmStatic
    var json: Json = buildJson()
        private set

    /**
     * 使用 VariableAdapter 注册
     *
     * @param adapter Variable 序列化适配器
     */
    @JvmStatic
    fun <T : Variable<*>> register(adapter: VariableAdapter<T>) {
        adapters[adapter.serialName] = adapter
    }

    /**
     * 重建 Json 实例
     *
     * 在注册完所有 Variable 子类后调用此方法
     */
    @JvmStatic
    fun rebuild() {
        json = buildJson()
    }

    /**
     * 检查是否已注册某个子类
     */
    @JvmStatic
    fun isRegistered(clazz: Class<out Variable<*>>): Boolean {
        return adapters.values.any { it.variableClass == clazz }
    }

    /**
     * 获取所有已注册的序列化名称
     */
    fun getRegisteredNames(): Set<String> {
        return adapters.keys.toSet()
    }

    /**
     * 尝试使用已注册的适配器将对象转换为 Variable
     *
     * @param value 要转换的对象
     * @return 转换后的 Variable，如果没有匹配的适配器则返回 null
     */
    @JvmStatic
    fun convert(value: Any): Variable<*>? {
        for (adapter in adapters.values) {
            if (adapter.canConvert(value)) {
                @Suppress("UNCHECKED_CAST")
                return (adapter as VariableAdapter<Variable<*>>).convert(value)
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildJson(): Json {
        val module = SerializersModule {
            polymorphic(Variable::class) {
                // 注册内置的 Variable 子类
                subclass(NullVariable::class, NullVariable.serializer())
                subclass(ByteVariable::class, ByteVariable.serializer())
                subclass(ShortVariable::class, ShortVariable.serializer())
                subclass(IntVariable::class, IntVariable.serializer())
                subclass(LongVariable::class, LongVariable.serializer())
                subclass(FloatVariable::class, FloatVariable.serializer())
                subclass(DoubleVariable::class, DoubleVariable.serializer())
                subclass(CharVariable::class, CharVariable.serializer())
                subclass(StringVariable::class, StringVariable.serializer())
                subclass(BooleanVariable::class, BooleanVariable.serializer())
                subclass(ArrayVariable::class, ArrayVariable.serializer())
                subclass(MapVariable::class, MapVariable.serializer())

                // 注册外部模块的 Variable 子类
                adapters.forEach { (_, adapter) ->
                    val serializer = AdapterSerializer(adapter as VariableAdapter<Variable<*>>)
                    subclass(adapter.variableClass.kotlin, serializer)
                }
            }
        }

        return Json {
            serializersModule = module
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    /**
     * 将 VariableAdapter 包装为 KSerializer
     */
    private class AdapterSerializer<T : Variable<*>>(
        private val adapter: VariableAdapter<T>
    ) : KSerializer<T> {

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(adapter.serialName)

        override fun serialize(encoder: Encoder, value: T) {
            val jsonString = adapter.serialize(value)
            val jsonElement = Json.parseToJsonElement(jsonString)
            (encoder as JsonEncoder).encodeJsonElement(jsonElement)
        }

        override fun deserialize(decoder: Decoder): T {
            val jsonElement = (decoder as JsonDecoder).decodeJsonElement()
            // 移除多态序列化添加的 type 字段
            val cleanedElement = if (jsonElement is JsonObject && "type" in jsonElement) {
                JsonObject(jsonElement.filterKeys { it != "type" })
            } else {
                jsonElement
            }
            return adapter.deserialize(cleanedElement.toString())
        }
    }

    @Awake(LifeCycle.LOAD)
    fun init() {
        rebuild()
    }
}
