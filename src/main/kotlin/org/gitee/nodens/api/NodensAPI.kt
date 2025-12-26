package org.gitee.nodens.api

import kotlinx.coroutines.*
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.api.interfaces.IAttributeAPI
import org.gitee.nodens.api.interfaces.IItemAPI
import org.gitee.nodens.api.interfaces.INodensAPI
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeData
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.entity.EntityAttributeMemory
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.module.item.VariableRegistry
import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.expansion.AsyncDispatcher
import kotlin.time.Duration.Companion.seconds

@RuntimeDependencies(
    RuntimeDependency(
        "!com.github.ben-manes.caffeine:caffeine:2.9.3",
        test = "!org.gitee.nodens.caffeine.cache.Caffeine",
        relocate = ["!com.github.benmanes.caffeine", "!org.gitee.nodens.caffeine"],
        transitive = false
    ),
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
    ),
    RuntimeDependency(
        "!com.eatthepath:fast-uuid:0.2.0",
        test = "!org.gitee.nodens.eatthepath.uuid.FastUUID",
        relocate = ["!com.eatthepath.uuid", "!org.gitee.nodens.eatthepath.uuid"],
        transitive = false
    ),
    RuntimeDependency(
        "!org.xerial.snappy:snappy-java:1.1.10.7",
        test = "!org.xerial.snappy.Snappy",
        transitive = false
    )
)
class NodensAPI: INodensAPI {

    override val itemAPI: IItemAPI
        get() = PlatformFactory.getAPI<IItemAPI>()

    override val attributeAPI: IAttributeAPI
        get() = PlatformFactory.getAPI<IAttributeAPI>()

    override val variableRegistry: VariableRegistry
        get() = VariableRegistry

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

        private val pluginJob = SupervisorJob()
        internal val pluginScope = CoroutineScope(AsyncDispatcher + pluginJob)

        @Awake(LifeCycle.DISABLE)
        private fun release() {
            shutdownScopes(10)
        }

        /**
         * 优雅关闭所有协程作用域
         * @param timeout 等待协程完成的超时时间（秒）
         */
        internal fun shutdownScopes(timeout: Long = 5) {
            runBlocking {
                // 先取消所有子协程，给它们发送取消信号
                pluginJob.cancelChildren()

                // 等待子协程完成，带超时保护
                try {
                    withTimeout(timeout.seconds) {
                        pluginJob.children.forEach { it.join() }
                    }
                } catch (_: TimeoutCancellationException) {
                    // 超时后强制取消
                }

                // 最终取消整个作用域
                pluginJob.cancel()
            }
        }
    }
}