package org.gitee.nodens.core

import org.gitee.nodens.api.Nodens
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.FastMatchingMap
import org.gitee.nodens.core.attribute.JavaScript
import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.util.ConfigLazy
import org.gitee.nodens.util.debug
import org.gitee.nodens.util.files
import org.gitee.nodens.util.mergeValues
import taboolib.common.LifeCycle
import taboolib.common.io.runningClassesWithoutLibrary
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration
import java.lang.reflect.Modifier
import java.math.BigDecimal

object AttributeManager {

    internal val groupMap = hashMapOf<String, IAttributeGroup>()

    private val attributeNumberConfigs = hashMapOf<String, HashMap<String, AttributeConfig>>()
    internal val ATTRIBUTE_MATCHING_MAP = FastMatchingMap<IAttributeGroup.Number>()

    val healthScaled by ConfigLazy(Nodens.config) { getBoolean("healthScaled", true) }

    @Reload(0)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        groupMap.clear()
        attributeNumberConfigs.clear()
        val list = mutableListOf<String>()
        runningClassesWithoutLibrary.forEach {
            if (it.hasInterface(IAttributeGroup::class.java)) {
                val group = (it.getInstance() as IAttributeGroup)
                groupMap[group.name] = group
                list += group.name + ".yml"
            }
        }
        files("attribute", *list.toTypedArray()) {
            val map = attributeNumberConfigs.getOrPut(it.nameWithoutExtension) { hashMapOf() }
            val configuration = Configuration.loadFromFile(it)
            configuration.getKeys(false).forEach { key ->
                map[key] = AttributeConfig(configuration.getConfigurationSection(key)!!)
            }
        }
        ATTRIBUTE_MATCHING_MAP.clear()
        runningClassesWithoutLibrary.forEach {
            if (it.hasInterface(IAttributeGroup::class.java)) {
                for (clazz in it.toClass().declaredClasses) {
                    // 检查是否是静态内部类（Kotlin的object对应Java的静态内部类）
                    if (Modifier.isStatic(clazz.modifiers)) {
                        try {
                            // 获取INSTANCE字段（Kotlin为object生成的单例字段）
                            val instanceField = clazz.getDeclaredField("INSTANCE")
                            instanceField.isAccessible = true // 确保可访问private字段
                            val instance = instanceField.get(null) // 静态字段，传入null

                            // 类型检查并添加到结果列表
                            if (instance is IAttributeGroup.Number) {
                                instance.config.keys.forEach { key ->
                                    ATTRIBUTE_MATCHING_MAP.put(key, instance)
                                    debug("&e┣&7AttributeKey $key loaded &a√".colored())
                                }
                            }
                        } catch (_: NoSuchFieldException) {
                            // 没有INSTANCE字段，说明不是object，跳过
                        } catch (e: IllegalAccessException) {
                            // 处理访问异常
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        JavaScript.reload()
        info("&e┣&7AttributeMatchingMap loaded &a√".colored())
    }

    fun getConfig(group: String, key: String): AttributeConfig {
        return attributeNumberConfigs[group]?.get(key) ?: error("未找到属性配置group: $group, key: $key")
    }

    fun matchAttribute(attribute: String): IAttributeData? {
        return ATTRIBUTE_MATCHING_MAP.getMatchResult(attribute)?.let { matchResult ->
            val remain = matchResult.remain ?: return null
            val parser = DigitalParser(remain, matchResult.value)
            AttributeData(matchResult.value, parser.getValue())
        }
    }

    fun getCombatPower(vararg data: IAttributeData): Double {
        var value = 0.0
        data.groupBy { it.attributeNumber }.forEach {
            val map = mergeValues(*it.value.map { d -> d.value }.toTypedArray())
            value += it.key.combatPower(map)
        }
        return BigDecimal.valueOf(value).setScale(2).toDouble()
    }

    fun getGroup(group: String): IAttributeGroup? {
        return groupMap[group]
    }

    fun getNumber(group: String, key: String): IAttributeGroup.Number? {
        return groupMap[group]?.numbers[key]
    }
}