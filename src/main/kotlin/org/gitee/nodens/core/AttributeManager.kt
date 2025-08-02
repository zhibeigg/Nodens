package org.gitee.nodens.core

import org.gitee.nodens.api.Nodens
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.FastMatchingMap
import org.gitee.nodens.core.attribute.JavaScript
import org.gitee.nodens.core.attribute.Mapping
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
        // 加载所有属性的配置文件
        files("attribute", *list.toTypedArray()) {
            val map = attributeNumberConfigs.getOrPut(it.nameWithoutExtension) { hashMapOf() }
            val configuration = Configuration.loadFromFile(it)
            configuration.getKeys(false).forEach { key ->
                map[key] = AttributeConfig(configuration.getConfigurationSection(key)!!)
            }
        }
        // 加载 Mapping 属性
        Mapping.numbers.clear()
        attributeNumberConfigs[Mapping.name]?.forEach {
            Mapping.numbers[it.key] = Mapping.MappingAttribute(it.key)
        }
        // 创建 MatchMap
        ATTRIBUTE_MATCHING_MAP.clear()
        runningClassesWithoutLibrary.forEach {
            if (it.hasInterface(IAttributeGroup::class.java)) {
                val instance = it.getInstance() as IAttributeGroup
                instance.numbers.forEach { (_, number) ->
                    number.config.keys.forEach { key ->
                        ATTRIBUTE_MATCHING_MAP.put(key, number)
                        debug("&e┣&7AttributeKey $key loaded &a√".colored())
                    }
                }
            }
        }
        // 加载 Js 属性
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