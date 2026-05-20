package org.gitee.nodens.core

import org.gitee.nodens.api.AttributeRegistrationConfig
import taboolib.library.configuration.ConfigurationSection
import java.lang.reflect.Proxy

class AttributeConfig(configurationSection: ConfigurationSection): ConfigurationSection by configurationSection {

    constructor(registrationConfig: AttributeRegistrationConfig): this(registrationConfig.toConfigurationSection())

    // 识别key
    val keys: List<String> = configurationSection.getStringList("keys")

    // 值类型
    val valueType: IAttributeGroup.Number.ValueType = IAttributeGroup.Number.ValueType.valueOf(
        (configurationSection.getString("valueType", "SINGLE") ?: "SINGLE").uppercase()
    )

    // 战斗力倍数
    val combatPower: Double = configurationSection.getDouble("combatPower")

    // 同步优先级
    val syncPriority: Int = configurationSection.getInt("syncPriority")

    // 操作优先级
    val handlePriority: Int = configurationSection.getInt("handlePriority")

    companion object {

        private fun AttributeRegistrationConfig.toConfigurationSection(): ConfigurationSection {
            val config = this
            val valueMap = linkedMapOf<String, Any>(
                "keys" to config.keys,
                "valueType" to config.valueType.name,
                "combatPower" to config.combatPower,
                "syncPriority" to config.syncPriority,
                "handlePriority" to config.handlePriority,
            )
            return Proxy.newProxyInstance(
                ConfigurationSection::class.java.classLoader,
                arrayOf(ConfigurationSection::class.java),
            ) { _, method, args ->
                val path = args?.firstOrNull() as? String
                when (method.name) {
                    "getStringList" -> if (path == "keys") config.keys else emptyList<String>()
                    "getString" -> when (path) {
                        "valueType" -> config.valueType.name
                        else -> args?.getOrNull(1) as? String
                    }
                    "getDouble" -> when (path) {
                        "combatPower" -> config.combatPower
                        else -> (args?.getOrNull(1) as? Number)?.toDouble() ?: 0.0
                    }
                    "getInt" -> when (path) {
                        "syncPriority" -> config.syncPriority
                        "handlePriority" -> config.handlePriority
                        else -> (args?.getOrNull(1) as? Number)?.toInt() ?: 0
                    }
                    "contains", "isSet" -> valueMap.containsKey(path)
                    "getKeys" -> valueMap.keys
                    "getValues", "toMap" -> valueMap.toMap()
                    "getName" -> "RuntimeAttributeConfig"
                    "toString" -> "AttributeRegistrationConfig($valueMap)"
                    "hashCode" -> config.hashCode()
                    "equals" -> false
                    else -> defaultReturn(method.returnType)
                }
            } as ConfigurationSection
        }

        private fun defaultReturn(type: Class<*>): Any? {
            return when (type) {
                java.lang.Boolean.TYPE -> false
                java.lang.Byte.TYPE -> 0.toByte()
                java.lang.Short.TYPE -> 0.toShort()
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Float.TYPE -> 0F
                java.lang.Double.TYPE -> 0.0
                java.lang.Character.TYPE -> '\u0000'
                java.lang.Void.TYPE -> null
                else -> null
            }
        }
    }
}
