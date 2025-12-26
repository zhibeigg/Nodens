package org.gitee.nodens.api

import org.bukkit.entity.LivingEntity
import org.gitee.nodens.api.interfaces.IAttributeAPI
import org.gitee.nodens.api.interfaces.IAttributeAPI.AttributeFinalValue
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.util.comparePriority
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common5.format

/**
 * 属性信息 API 实现
 * 提供快速获取实体属性信息的便捷方法
 */
class NodensAttributeAPI : IAttributeAPI {

    override fun getAllAttributes(
        entity: LivingEntity,
        transferMap: Boolean
    ): Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>? {
        return entity.attributeMemory()?.mergedAllAttribute(transferMap)
    }

    override fun getSortedAttributes(
        entity: LivingEntity,
        transferMap: Boolean
    ): List<Pair<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>>? {
        return entity.attributeMemory()?.mergedAllAttribute(transferMap)?.toSortedMap { o1, o2 ->
            val priorityCompare = comparePriority(o1.config.handlePriority, o2.config.handlePriority)
            if (priorityCompare != 0) priorityCompare else o1.name.compareTo(o2.name)
        }?.toList()
    }

    override fun getAttribute(
        entity: LivingEntity,
        attribute: IAttributeGroup.Number,
        transferMap: Boolean
    ): Map<DigitalParser.Type, DoubleArray>? {
        return entity.attributeMemory()?.mergedAttribute(attribute, transferMap)
    }

    override fun getFinalValue(
        entity: LivingEntity,
        attribute: IAttributeGroup.Number
    ): AttributeFinalValue? {
        val memory = entity.attributeMemory() ?: return null
        val valueMap = memory.mergedAttribute(attribute, true)
        if (valueMap.isEmpty()) return null

        val result = attribute.getFinalValue(entity, valueMap)
        return AttributeFinalValue(
            type = attribute.config.valueType,
            value = result.value,
            rangeValue = result.rangeValue?.let { it.left to it.right }
        )
    }

    override fun getAllFinalValues(
        entity: LivingEntity,
        transferMap: Boolean
    ): Map<String, AttributeFinalValue>? {
        val memory = entity.attributeMemory() ?: return null
        val attributes = memory.mergedAllAttribute(transferMap)

        return attributes.mapNotNull { (attr, valueMap) ->
            val result = attr.getFinalValue(entity, valueMap)
            val key = "${attr.group.name}:${attr.name}"
            val finalValue = AttributeFinalValue(
                type = attr.config.valueType,
                value = result.value,
                rangeValue = result.rangeValue?.let { it.left to it.right }
            )
            key to finalValue
        }.toMap()
    }

    override fun getAllAttributeStrings(
        entity: LivingEntity,
        transferMap: Boolean
    ): Map<String, String>? {
        val memory = entity.attributeMemory() ?: return null
        val attributes = memory.mergedAllAttribute(transferMap)

        return attributes.map { (attr, valueMap) ->
            val key = "${attr.group.name}:${attr.name}"
            val countStr = valueMap[DigitalParser.Type.COUNT]?.joinToString("-") { it.format(1).toString() } ?: "0"
            val percentStr = valueMap[DigitalParser.Type.PERCENT]?.joinToString("-") { (it * 100).format(1).toString() } ?: "0"
            key to "$countStr + $percentStr%"
        }.toMap()
    }

    override fun getCombatPower(entity: LivingEntity): Double? {
        return entity.attributeMemory()?.getCombatPower()?.values?.sum()
    }

    override fun getCombatPowerBreakdown(entity: LivingEntity): Map<IAttributeGroup.Number, Double>? {
        return entity.attributeMemory()?.getCombatPower()
    }

    override fun getAttributeNumber(groupName: String, attributeName: String): IAttributeGroup.Number? {
        return AttributeManager.getNumber(groupName, attributeName)
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<IAttributeAPI>(NodensAttributeAPI())
        }

        /**
         * 获取 API 实例
         */
        @JvmStatic
        fun getInstance(): IAttributeAPI = PlatformFactory.getAPI()

        /**
         * 快速获取实体的所有属性字符串（用于 UI 显示）
         * @param entity 目标实体
         * @return 属性名到值字符串的映射
         */
        @JvmStatic
        fun getAttributeStrings(entity: LivingEntity): Map<String, String> {
            return getInstance().getAllAttributeStrings(entity) ?: emptyMap()
        }

        /**
         * 快速获取实体的战斗力
         * @param entity 目标实体
         * @return 战斗力值
         */
        @JvmStatic
        fun getCombatPower(entity: LivingEntity): Double {
            return getInstance().getCombatPower(entity) ?: 0.0
        }

        /**
         * 快速获取实体特定属性的最终值
         * @param entity 目标实体
         * @param groupName 属性组名
         * @param attributeName 属性名
         * @return 最终值字符串
         */
        @JvmStatic
        fun getAttributeValue(entity: LivingEntity, groupName: String, attributeName: String): String {
            val api = getInstance()
            val attr = api.getAttributeNumber(groupName, attributeName) ?: return "0"
            return api.getFinalValue(entity, attr)?.toString() ?: "0"
        }

        /**
         * 快速获取实体特定属性的详细字符串
         * @param entity 目标实体
         * @param groupName 属性组名
         * @param attributeName 属性名
         * @return 详细字符串（格式：COUNT + PERCENT%）
         */
        @JvmStatic
        fun getAttributeDetail(entity: LivingEntity, groupName: String, attributeName: String): String {
            val key = "$groupName:$attributeName"
            return getInstance().getAllAttributeStrings(entity)?.get(key) ?: "0 + 0%"
        }
    }
}
