package org.gitee.nodens.api.interfaces

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.api.NodensItemAPI
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.core.AttributeData
import org.gitee.nodens.core.IAttributeData
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.module.item.VariableRegistry

interface INodensAPI {

    val itemAPI: IItemAPI

    val variableRegistry: VariableRegistry

    /**
     * 攻击实体
     * @param damageProcessor 攻击处理器(上下文)
     * */
    fun attackEntity(damageProcessor: DamageProcessor): EntityDamageByEntityEvent?

    /**
     * 添加临时属性
     *
     * @param entity 添加属性的实体
     * @param key 添加的属性 ID
     * @param tempAttributeData 临时属性数据
     * */
    fun addTempAttribute(entity: LivingEntity, key: String, tempAttributeData: TempAttributeData)

    /**
     * 移除临时属性
     *
     * @param entity 移除属性的实体
     * @param key 移除的属性 ID
     * */
    fun removeTempAttribute(entity: LivingEntity, key: String)

    /**
     * 获得实体的属性寄存器
     *
     * @param entity 实体
     * @return 实体属性寄存器[EntityAttributeMemory]
     * */
    fun getAttributeMemory(entity: LivingEntity): EntityAttributeMemory?

    /**
     * 匹配字符串中的属性
     * @param attribute 含有属性信息的字符串
     * @return 匹配到的属性数据
     * */
    fun matchAttribute(attribute: String): IAttributeData?

    /**
     * 匹配字符串列表中的属性
     * @param attributes 含有属性信息的字符串列表
     * @return 匹配到的属性数据列表
     * */
    fun matchAttributes(attributes: List<String>): List<IAttributeData>

    /**
     * 更新实体属性
     * */
    fun updateAttribute(entity: LivingEntity)
}