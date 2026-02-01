package org.gitee.nodens.core

import org.gitee.nodens.api.Nodens
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.FastMatchingMap
import org.gitee.nodens.core.attribute.JavaScript
import org.gitee.nodens.core.attribute.Mapping
import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.util.*
import taboolib.common.LifeCycle
import taboolib.common.io.runningClassesWithoutLibrary
import taboolib.common.platform.Awake
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.ReloadAwareLazy
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

object AttributeManager {

    /** 使用 ConcurrentHashMap 保证线程安全 */
    @Volatile
    internal var groupMap = ConcurrentHashMap<String, IAttributeGroup>()
        private set

    @Volatile
    private var attributeNumberConfigs = ConcurrentHashMap<String, ConcurrentHashMap<String, AttributeConfig>>()

    internal val ATTRIBUTE_MATCHING_MAP = FastMatchingMap<IAttributeGroup.Number>()

    val healthScaled by ReloadAwareLazy(Nodens.config) { Nodens.config.getBoolean("healthScaled", true) }

    @Reload(0)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        // 使用新的 Map 进行原子替换，避免读取时数据不一致
        val newGroupMap = ConcurrentHashMap<String, IAttributeGroup>()
        val newAttributeNumberConfigs = ConcurrentHashMap<String, ConcurrentHashMap<String, AttributeConfig>>()
        val list = mutableListOf<String>()

        runningClassesWithoutLibrary.forEach {
            if (it.hasInterface(IAttributeGroup::class.java)) {
                val group = (it.getInstance() as IAttributeGroup)
                newGroupMap[group.name] = group
                list += group.name + ".yml"
            }
        }
        // 加载所有属性的配置文件
        consoleMessage("")
        consoleMessage("&6╭─────────────────────────────────────────")
        consoleMessage("&6│ &e⚡ &f属性系统加载中...")
        consoleMessage("&6├─────────────────────────────────────────")
        files("attribute", *list.toTypedArray()) {
            val map = newAttributeNumberConfigs.getOrPut(it.nameWithoutExtension) { ConcurrentHashMap() }
            val configuration = Configuration.loadFromFile(it)
            val keys = configuration.getKeys(false)
            consoleMessage("&6│ &7├ &e${it.name} &8» &7${keys.size}个配置项")
            keys.forEach { key ->
                val section = configuration.getConfigurationSection(key)
                if (section != null) {
                    map[key] = AttributeConfig(section)
                }
            }
        }

        // 原子替换
        groupMap = newGroupMap
        attributeNumberConfigs = newAttributeNumberConfigs

        // 加载 Mapping 属性
        Mapping.numbers.clear()
        attributeNumberConfigs[Mapping.name]?.forEach {
            Mapping.numbers[it.key] = Mapping.MappingAttribute(it.key)
        }
        // 创建 MatchMap
        ATTRIBUTE_MATCHING_MAP.clear()
        var totalKeys = 0
        consoleMessage("&6├─────────────────────────────────────────")
        consoleMessage("&6│ &e📦 &f属性组注册")
        runningClassesWithoutLibrary.forEach {
            if (it.hasInterface(IAttributeGroup::class.java)) {
                val instance = it.getInstance() as IAttributeGroup
                consoleMessage("&6│ &7├ &b${instance.name} &8» &7${instance.numbers.size}个属性")
                instance.numbers.forEach { (name, number) ->
                    try {
                        val keys = number.config.keys
                        consoleMessage("&6│ &7│ &7└ &a$name &8(&7${keys.size} keys&8)")
                        keys.forEach { key ->
                            ATTRIBUTE_MATCHING_MAP.put(key, number)
                            totalKeys++
                        }
                    } catch (e: Exception) {
                        consoleMessage("&6│ &7│ &7└ &c✘ $name &8- &c${e.message}")
                    }
                }
            }
        }
        consoleMessage("&6├─────────────────────────────────────────")
        consoleMessage("&6│ &7总计: &f$totalKeys &7个属性匹配键")
        // 加载 Js 属性
        JavaScript.reload()
        consoleMessage("&6╰─────────────────────────────────────────")
        consoleMessage("&a✔ &f属性系统加载完成!")
        consoleMessage("")
    }

    fun getConfig(group: String, key: String): AttributeConfig {
        return attributeNumberConfigs[group]?.get(key) ?: error("未找到属性配置group: $group, key: $key")
    }

    fun matchAttribute(attribute: String): IAttributeData? {
        return ATTRIBUTE_MATCHING_MAP.getMatchResult(attribute)?.let { matchResult ->
            val remain = matchResult.remain ?: return null
            val parser = DigitalParser(remain, matchResult.value)
            if (parser.getValue().isEmpty()) return null
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