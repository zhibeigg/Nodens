package org.gitee.nodens.util

import org.bukkit.entity.Player
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptFrame
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
    return script().sender?.castSafely<Player>() ?: error("Orryx脚本中Sender不是玩家")
}

internal fun ScriptContext.bukkitPlayer(): Player {
    return sender?.castSafely<Player>() ?: error("Orryx脚本中Sender不是玩家")
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