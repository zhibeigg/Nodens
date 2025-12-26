package org.gitee.nodens.api.interfaces

import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.IAttributeGroup

/**
 * 属性信息 API 接口
 * 提供快速获取实体属性信息的方法
 */
interface IAttributeAPI {

    /**
     * 获取实体的所有合并后属性
     * @param entity 目标实体
     * @param transferMap 是否转换 Mapping 属性为实际属性
     * @return 属性映射表，key 为属性，value 为属性值（包含 COUNT 和 PERCENT）
     */
    fun getAllAttributes(entity: LivingEntity, transferMap: Boolean = true): Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>?

    /**
     * 获取实体的所有属性（按优先级排序）
     * @param entity 目标实体
     * @param transferMap 是否转换 Mapping 属性
     * @return 按优先级排序的属性列表
     */
    fun getSortedAttributes(entity: LivingEntity, transferMap: Boolean = true): List<Pair<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>>?

    /**
     * 获取实体的特定属性值
     * @param entity 目标实体
     * @param attribute 要查询的属性
     * @param transferMap 是否转换 Mapping 属性
     * @return 属性值映射，包含 COUNT 和 PERCENT
     */
    fun getAttribute(entity: LivingEntity, attribute: IAttributeGroup.Number, transferMap: Boolean = true): Map<DigitalParser.Type, DoubleArray>?

    /**
     * 获取实体特定属性的最终计算值
     * @param entity 目标实体
     * @param attribute 要查询的属性
     * @return 最终值（对于单值属性返回单个值，对于范围属性返回范围）
     */
    fun getFinalValue(entity: LivingEntity, attribute: IAttributeGroup.Number): AttributeFinalValue?

    /**
     * 获取实体所有属性的最终计算值
     * @param entity 目标实体
     * @param transferMap 是否转换 Mapping 属性
     * @return 属性名到最终值的映射
     */
    fun getAllFinalValues(entity: LivingEntity, transferMap: Boolean = true): Map<String, AttributeFinalValue>?

    /**
     * 获取实体所有属性的详细字符串表示
     * @param entity 目标实体
     * @param transferMap 是否转换 Mapping 属性
     * @return 属性名到详细字符串的映射（格式：COUNT + PERCENT%）
     */
    fun getAllAttributeStrings(entity: LivingEntity, transferMap: Boolean = true): Map<String, String>?

    /**
     * 获取实体的战斗力
     * @param entity 目标实体
     * @return 总战斗力
     */
    fun getCombatPower(entity: LivingEntity): Double?

    /**
     * 获取实体各属性的战斗力分布
     * @param entity 目标实体
     * @return 属性到战斗力的映射
     */
    fun getCombatPowerBreakdown(entity: LivingEntity): Map<IAttributeGroup.Number, Double>?

    /**
     * 根据属性组名和属性名获取属性
     * @param groupName 属性组名（如 "Health", "Damage"）
     * @param attributeName 属性名（如 "Max", "Physics"）
     * @return 属性对象，不存在则返回 null
     */
    fun getAttributeNumber(groupName: String, attributeName: String): IAttributeGroup.Number?

    /**
     * 属性最终值数据类
     */
    data class AttributeFinalValue(
        val type: IAttributeGroup.Number.ValueType,
        val value: Double?,
        val rangeValue: Pair<Double, Double>?
    ) {
        /** 是否为单值类型 */
        val isSingle: Boolean get() = type == IAttributeGroup.Number.ValueType.SINGLE

        /** 是否为范围类型 */
        val isRange: Boolean get() = type == IAttributeGroup.Number.ValueType.RANGE

        override fun toString(): String {
            return if (isSingle) {
                value?.toString() ?: "0"
            } else {
                "${rangeValue?.first ?: 0} - ${rangeValue?.second ?: 0}"
            }
        }
    }
}
