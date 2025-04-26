package org.gitee.nodens.core.kether

import org.gitee.nodens.module.item.ItemConfig
import org.gitee.nodens.module.item.NormalContext
import org.gitee.nodens.module.random.RandomManager
import org.gitee.nodens.util.NODENS_NAMESPACE
import org.gitee.nodens.util.nodensEnvironmentNamespaces
import org.gitee.nodens.util.toVariable
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.console
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

object ItemActions {

    @KetherProperty(bind = ItemConfig::class)
    fun propertyItemConfig() = object : ScriptProperty<ItemConfig>("ItemConfig.operator") {

        override fun read(instance: ItemConfig, key: String): OpenResult {
            return when (key) {
                "key" -> OpenResult.successful(instance.key)
                "skillOwner" -> OpenResult.successful(instance.skullOwner)
                "skillTexture" -> OpenResult.successful(instance.skullTexture)
                "unbreakable" -> OpenResult.successful(instance.isUnBreakable)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: ItemConfig, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    @KetherProperty(bind = NormalContext::class)
    fun propertyContext() = object : ScriptProperty<NormalContext>("NormalGenerator.NormalContext.operator") {

        override fun read(instance: NormalContext, key: String): OpenResult {
            return when (key) {
                "key" -> OpenResult.successful(instance.key)
                else -> OpenResult.successful(instance.variable[key])
            }
        }

        override fun write(instance: NormalContext, key: String, value: Any?): OpenResult {
            return try {
                instance.variable[key] = value?.toVariable()!!
                OpenResult.successful()
            } catch (_: Throwable) {
                OpenResult.failed()
            }
        }
    }

    @KetherParser(["randoms"], namespace = NODENS_NAMESPACE, shared = true)
    private fun randomsParser() = combinationParser {
        it.group(
            text()
        ).apply(it) { id ->
            future {
                val context = script().get<NormalContext>("itemContext") ?: return@future completedFuture(null)
                randomsEval(script().sender, id, context)
            }
        }
    }

    fun randomsEval(sender: ProxyCommandSender?, randoms: String, context: NormalContext): CompletableFuture<Any?> {
        val randoms = RandomManager.randomsMap[randoms] ?: return CompletableFuture.completedFuture(null)
        return KetherShell.eval(
            randoms.action,
            ScriptOptions.builder().vars(context.variable.mapValues { it.value.value }).sender(sender ?: console()).namespace(nodensEnvironmentNamespaces).build()
        )
    }
}