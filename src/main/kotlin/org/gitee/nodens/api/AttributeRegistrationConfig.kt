package org.gitee.nodens.api

import org.gitee.nodens.core.IAttributeGroup

/**
 * 运行期属性注册配置。
 *
 * 外部插件可使用该对象直接注册属性配置，不必再伪造 TabooLib 的 ConfigurationSection。
 */
data class AttributeRegistrationConfig(
    /** 属性 Lore 匹配关键字 */
    val keys: List<String>,
    /** 属性值类型 */
    val valueType: IAttributeGroup.Number.ValueType = IAttributeGroup.Number.ValueType.SINGLE,
    /** 战斗力倍率 */
    val combatPower: Double = 0.0,
    /** 同步优先级 */
    val syncPriority: Int = 0,
    /** 处理优先级 */
    val handlePriority: Int = 1,
)
