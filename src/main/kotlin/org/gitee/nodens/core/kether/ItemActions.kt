package org.gitee.nodens.core.kether

import org.bukkit.inventory.ItemStack
import org.gitee.nodens.module.item.ItemConfig
import org.gitee.nodens.module.item.NormalContext
import org.gitee.nodens.module.item.action.ActionContext
import org.gitee.nodens.module.item.group.GroupManager
import org.gitee.nodens.module.formula.FormulaManager
import org.gitee.nodens.util.NODENS_NAMESPACE
import org.gitee.nodens.util.bukkitPlayer
import org.gitee.nodens.util.context
import org.gitee.nodens.util.toVariable
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
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

    @KetherParser(["formula"], namespace = NODENS_NAMESPACE, shared = true)
    private fun formulaParser() = combinationParser {
        it.group(
            text()
        ).apply(it) { id ->
            future { formulaEval(script().sender, id, variables().toMap()) }
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


    @KetherProperty(bind = ActionContext::class)
    fun propertyActionContext() = object : ScriptProperty<ActionContext>("ActionContext.operator") {

        override fun read(instance: ActionContext, key: String): OpenResult {
            return when (key) {
                "player" -> OpenResult.successful(instance.player)
                "event" -> OpenResult.successful(instance.event)
                "trigger", "triggerId" -> OpenResult.successful(instance.triggerId)
                else -> {
                    val value = instance.get<Any>(key)
                    if (value != null) OpenResult.successful(value) else OpenResult.failed()
                }
            }
        }

        override fun write(instance: ActionContext, key: String, value: Any?): OpenResult {
            instance[key] = value
            return OpenResult.successful()
        }
    }

    fun formulaEval(sender: ProxyCommandSender?, id: String, map: Map<String, Any?>): CompletableFuture<Any?> {
        val formula = FormulaManager.formulaMap[id] ?: return CompletableFuture.completedFuture(null)
        return formula.eval(sender, map)
    }
}