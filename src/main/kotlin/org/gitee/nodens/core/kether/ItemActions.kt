package org.gitee.nodens.core.kether

import org.bukkit.inventory.ItemStack
import org.gitee.nodens.module.item.ItemConfig
import org.gitee.nodens.module.item.NormalContext
import org.gitee.nodens.module.item.group.GroupManager
import org.gitee.nodens.module.random.RandomManager
import org.gitee.nodens.util.NODENS_NAMESPACE
import org.gitee.nodens.util.bukkitPlayer
import org.gitee.nodens.util.context
import org.gitee.nodens.util.nodensEnvironmentNamespaces
import org.gitee.nodens.util.toVariable
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.console
import taboolib.module.chat.uncolored
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
    fun propertyContext() = object : ScriptProperty<NormalContext>("NormalContext.operator") {

        override fun read(instance: NormalContext, key: String): OpenResult {
            return when {
                key.startsWith("@") -> OpenResult.successful(instance[key.substring(1)])
                key == "key" -> OpenResult.successful(instance.key)
                else -> OpenResult.successful(instance[key])
            }
        }

        override fun write(instance: NormalContext, key: String, value: Any?): OpenResult {
            return try {
                instance[key] = value
                OpenResult.successful()
            } catch (_: Throwable) {
                OpenResult.failed()
            }
        }
    }

    @KetherParser(["unColor"], shared = true)
    private fun unColor() = combinationParser {
        it.group(
            text()
        ).apply(it) { text ->
            now {
                text.uncolored()
            }
        }
    }

    @KetherParser(["itemVariable"], shared = true)
    private fun variable() = scriptParser {
        val key = it.nextToken()
        val itemStack = it.nextParsedAction()
        actionFuture { future ->
            run(itemStack).thenApply { itemStack ->
                val itemStack = itemStack as ItemStack?
                future.complete(itemStack?.context()?.get(key))
            }
        }
    }

    @KetherParser(["randoms"], namespace = NODENS_NAMESPACE, shared = true)
    private fun randomsParser() = combinationParser {
        it.group(
            text()
        ).apply(it) { id ->
            future {
                val context = script().get<NormalContext>("context") ?: return@future completedFuture(null)
                randomsEval(script().sender, id, context)
            }
        }
    }

    @KetherParser(["itemGroup"], shared = true)
    private fun itemGroup() = scriptParser {
        val group = it.nextToken()
        it.switch {
            case("variable") {
                val key = it.nextToken()
                val value = it.nextParsedAction()
                val any = it.nextParsedAction()
                actionFuture { future ->
                    run(value).str { value ->
                        run(any).bool { any ->
                            future.complete(
                                GroupManager.itemGroups.get(group)!!.check(script().bukkitPlayer(), any) {
                                    context()?.get(key) == value
                                }
                            )
                        }
                    }
                }
            }
        }
    }


    fun randomsEval(sender: ProxyCommandSender?, randoms: String, context: NormalContext): CompletableFuture<Any?> {
        val randoms = RandomManager.randomsMap[randoms] ?: return CompletableFuture.completedFuture(null)
        return KetherShell.eval(
            randoms.action,
            ScriptOptions.builder().vars(context.map()).sender(sender ?: console()).namespace(nodensEnvironmentNamespaces).build()
        )
    }
}