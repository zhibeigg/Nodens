package org.gitee.nodens.api

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.api.interfaces.INodensAPI
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory

class NodensAPI: INodensAPI {

    override fun attackEntity(damageProcessor: DamageProcessor): EntityDamageByEntityEvent? {
        return damageProcessor.callDamage()
    }

    override fun addTempAttribute(entity: LivingEntity, key: String, tempAttributeData: TempAttributeData) {
        entity.attributeMemory()?.addAttribute(key, tempAttributeData)
    }

    override fun removeTempAttribute(entity: LivingEntity, key: String) {
        entity.attributeMemory()?.removeAttribute(key)
    }

    override fun getAttributeMemory(entity: LivingEntity): EntityAttributeMemory? {
        return entity.attributeMemory()
    }
}