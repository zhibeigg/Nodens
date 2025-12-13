package org.gitee.nodens.module.item

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import kotlin.reflect.KClass

/**
 * Variable 注册中心
 *
 * 用于注册外部模块的 Variable 子类，使其支持 kotlinx.serialization 多态序列化。
 *
 * 使用示例：
 * ```kotlin
 * // 1. 定义你的 Variable 子类
 * @Serializable
 * @SerialName("SpiritItemVariable")
 * data class SpiritItemVariable(override val value: SpiritData) : Variable<SpiritData>
 *
 * // 2. 在插件加载时注册
 * VariableRegistry.register(SpiritItemVariable::class, SpiritItemVariable.serializer())
 *
 * // 3. 注册完成后调用 rebuild() 重建 Json 实例
 * VariableRegistry.rebuild()
 * ```
 *
 * 注意：
 * - 注册必须在使用序列化之前完成
 * - 建议在 TabooLib 的 LOAD 或 ENABLE 生命周期中注册
 * - 每次注册新的子类后，需要调用 rebuild() 方法
 */
object VariableRegistry {

    private val registeredSubclasses = mutableMapOf<KClass<out Variable<*>>, KSerializer<out Variable<*>>>()

    /**
     * 当前配置好的 Json 实例，支持已注册的 Variable 多态序列化
     */
    @Volatile
    var json: Json = buildJson()
        private set

    /**
     * 注册一个 Variable 子类
     *
     * @param kClass Variable 子类的 KClass
     * @param serializer 该子类的序列化器
     */
    fun <T : Variable<*>> register(kClass: KClass<T>, serializer: KSerializer<T>) {
        registeredSubclasses[kClass] = serializer
    }

    /**
     * 注册一个 Variable 子类（内联版本）
     *
     * @param serializer 该子类的序列化器
     */
    inline fun <reified T : Variable<*>> register(serializer: KSerializer<T>) {
        register(T::class, serializer)
    }

    /**
     * 批量注册 Variable 子类
     *
     * @param entries 子类和序列化器的映射
     */
    fun registerAll(entries: Map<KClass<out Variable<*>>, KSerializer<out Variable<*>>>) {
        registeredSubclasses.putAll(entries)
    }

    /**
     * 重建 Json 实例
     *
     * 在注册完所有 Variable 子类后调用此方法
     */
    fun rebuild() {
        json = buildJson()
    }

    /**
     * 检查是否已注册某个子类
     */
    fun isRegistered(kClass: KClass<out Variable<*>>): Boolean {
        return kClass in registeredSubclasses
    }

    /**
     * 获取所有已注册的子类
     */
    fun getRegisteredClasses(): Set<KClass<out Variable<*>>> {
        return registeredSubclasses.keys.toSet()
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
                registeredSubclasses.forEach { (kClass, serializer) ->
                    subclass(kClass as KClass<Variable<*>>, serializer as KSerializer<Variable<*>>)
                }
            }
        }

        return Json {
            serializersModule = module
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    @Awake(LifeCycle.LOAD)
    fun init() {
        // 初始化时构建默认的 Json 实例
        rebuild()
    }
}
