package org.gitee.nodens.core

import taboolib.library.configuration.ConfigurationSection

class AttributeConfig(configurationSection: ConfigurationSection): ConfigurationSection by configurationSection {

    // 识别key
    val keys: List<String> = configurationSection.getStringList("keys")

    // 值类型
    val valueType: IAttributeGroup.Number.ValueType = IAttributeGroup.Number.ValueType.valueOf(configurationSection.getString("valueType", "SINGLE")!!.uppercase())

    // 战斗力倍数
    val combatPower: Double = configurationSection.getDouble("combatPower")

    // 同步优先级
    val syncPriority: Int = configurationSection.getInt("syncPriority")

    // 操作优先级
    val handlePriority: Int = configurationSection.getInt("handlePriority")
}