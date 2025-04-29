package org.gitee.nodens.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.api.interfaces.INodensAPI
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeData
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Awake
import taboolib.expansion.AsyncDispatcher

@RuntimeDependencies(
    RuntimeDependency(
        "!org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
        test = "!org.gitee.nodens.serialization.Serializer",
        relocate = ["!kotlin.", "!kotlin2120.", "!kotlinx.serialization.", "!org.gitee.nodens.serialization."],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1",
        test = "!org.gitee.nodens.serialization.json.Json",
        relocate = ["!kotlin.", "!kotlin2120.", "!kotlinx.serialization.", "!org.gitee.nodens.serialization."],
        transitive = false
    )
)
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

    override fun matchAttribute(attribute: String): IAttributeData? {
        return AttributeManager.matchAttribute(attribute)
    }

    override fun matchAttributes(attributes: List<String>): List<IAttributeData> {
        return attributes.mapNotNull { matchAttribute(it) }
    }

    override fun updateAttribute(entity: LivingEntity) {
        entity.attributeMemory()?.updateAttributeAsync()
    }

    companion object {
        internal val pluginScope = CoroutineScope(AsyncDispatcher + SupervisorJob())

        @Awake(LifeCycle.DISABLE)
        private fun release() {
            pluginScope.cancel("服务器关闭")
        }
    }
}