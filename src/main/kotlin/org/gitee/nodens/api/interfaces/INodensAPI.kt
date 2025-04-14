package org.gitee.nodens.api.interfaces

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.entity.EntityAttributeMemory

interface INodensAPI {

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
}