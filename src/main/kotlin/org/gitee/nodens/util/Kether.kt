package org.gitee.nodens.util

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.entity.EntityAttributeMemory
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.library.kether.Parser
import taboolib.library.kether.Parser.Action
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.run
import taboolib.module.kether.script
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

const val NODENS_NAMESPACE = "Nodens"

val nodensEnvironmentNamespaces = listOf(NODENS_NAMESPACE, "kether")

val EMPTY_FUNCTION = {}

internal fun getBytes(actions: String): ByteArray {
    val s = if (actions.startsWith("def ")) actions else "def main = { $actions }"
    val texts = s.split("\n")
    return texts.mapNotNull { if (it.trim().startsWith("#")) null else it }.joinToString("\n").toByteArray(
        StandardCharsets.UTF_8
    )
}

internal fun ScriptFrame.bukkitPlayer(): Player {
    return script().sender?.castSafely<Player>() ?: error("Nodens脚本中Sender不是玩家")
}

internal fun ScriptContext.bukkitPlayer(): Player {
    return sender?.castSafely<Player>() ?: error("Nodens脚本中Sender不是玩家")
}

internal fun ScriptFrame.livingEntity(): LivingEntity {
    return script().sender?.castSafely<LivingEntity>() ?: error("Nodens脚本中Sender不是LivingEntity")
}

internal fun ScriptContext.livingEntity(): LivingEntity {
    return sender?.castSafely<LivingEntity>() ?: error("Nodens脚本中Sender不是LivingEntity")
}

internal fun number(): Parser<IAttributeGroup.Number> {
    return Parser.frame { r ->
        val action = r.nextParsedAction()
        Action {
            it.run(action).thenApply { number ->
                number as IAttributeGroup.Number
            }
        }
    }
}

internal fun attributeMemory(): Parser<EntityAttributeMemory> {
    return Parser.frame { r ->
        val action = r.nextParsedAction()
        Action {
            it.run(action).thenApply { number ->
                number as EntityAttributeMemory
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun damageProcessors(): Parser<List<DamageProcessor>> {
    return Parser.frame { r ->
        val action = r.nextParsedAction()
        Action { it.run(action).thenApply { obj -> if (obj is List<*>) obj as List<DamageProcessor> else listOf(obj as DamageProcessor) } }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun regainProcessors(): Parser<List<RegainProcessor>> {
    return Parser.frame { r ->
        val action = r.nextParsedAction()
        Action { it.run(action).thenApply { obj -> if (obj is List<*>) obj as List<RegainProcessor> else listOf(obj as RegainProcessor) } }
    }
}

/**
 * 确保[func]在主线程运行
 * */
fun <T> ensureWaitSync(func: () -> T): CompletableFuture<T> {
    if (isPrimaryThread) {
        return CompletableFuture.completedFuture(func())
    } else {
        val future = CompletableFuture<T>()
        submit { future.complete(func()) }
        return future
    }
}

fun <T> ensureSync(func: () -> T) {
    if (isPrimaryThread) {
        func()
    } else {
        submit { func() }
    }
}