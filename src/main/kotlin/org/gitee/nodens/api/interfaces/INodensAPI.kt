package org.gitee.nodens.api.interfaces

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.IAttributeData
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.module.item.VariableRegistry
import java.util.UUID

interface INodensAPI {

    val itemAPI: IItemAPI

    val attributeAPI: IAttributeAPI

    val reloadAPI: IReloadAPI

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
     * 获得实体的属性寄存器（别名，便于 Java/Kotlin 调用时表达更明确）
     *
     * @param entity 实体
     * @return 实体属性寄存器[EntityAttributeMemory]
     * */
    fun getEntityAttributeMemory(entity: LivingEntity): EntityAttributeMemory?

    /**
     * 确保实体拥有属性寄存器，不存在时立即创建。
     *
     * @param entity 实体
     * @return 实体属性寄存器[EntityAttributeMemory]
     * */
    fun ensureAttributeMemory(entity: LivingEntity): EntityAttributeMemory

    /**
     * 移除实体属性寄存器。
     *
     * @param entity 实体
     * @param resetHealth 是否在移除时恢复 Bukkit 生命属性
     * @return 被移除的实体属性寄存器，不存在则返回 null
     * */
    fun removeAttributeMemory(entity: LivingEntity, resetHealth: Boolean = true): EntityAttributeMemory?

    /**
     * 获取当前所有实体属性寄存器快照。
     * */
    fun getAttributeMemories(): Map<UUID, EntityAttributeMemory>

    /**
     * 更新当前所有实体属性。
     * */
    fun updateAllAttributes()

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

    /**
     * 注册运行期属性组。
     */
    fun registerAttributeGroup(group: IAttributeGroup, reloadAttributes: Boolean = true): IAttributeGroup?

    /**
     * 注销运行期属性组。
     */
    fun unregisterAttributeGroup(groupName: String, reloadAttributes: Boolean = true): IAttributeGroup?

    /**
     * 获取属性组。
     */
    fun getAttributeGroup(groupName: String): IAttributeGroup?

    /**
     * 获取全部属性组快照。
     */
    fun getAttributeGroups(): Map<String, IAttributeGroup>

    /**
     * 获取属性配置。
     */
    fun getAttributeConfig(groupName: String, attributeName: String): AttributeConfig?

    /**
     * 重载全部模块。
     */
    fun reload()

    /**
     * 重载主配置 config.yml。
     */
    fun reloadConfig()

    /**
     * 重载属性配置与匹配表。
     */
    fun reloadAttributes(updateEntities: Boolean = true)

    /**
     * 重载 handle.yml 伤害/恢复脚本。
     */
    fun reloadHandle()

    /**
     * 重载物品配置。
     */
    fun reloadItems()

    /**
     * 重载物品组配置。
     */
    fun reloadItemGroups()

    /**
     * 重载条件匹配配置。
     */
    fun reloadConditions()

    /**
     * 重载随机动作配置。
     */
    fun reloadRandoms()

    /**
     * 重载自然恢复任务。
     */
    fun reloadRegainTask()

    /**
     * 执行指定权重的重载函数。
     */
    fun reloadByWeight(weight: Int)

    /**
     * 执行多个指定权重的重载函数。
     */
    fun reloadByWeights(vararg weights: Int)
}