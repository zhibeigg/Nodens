package org.gitee.nodens.util

import org.gitee.nodens.api.Nodens
import taboolib.common.platform.function.console
import taboolib.module.chat.colored
import taboolib.module.configuration.util.ReloadAwareLazy

val debug: Boolean by ReloadAwareLazy(Nodens.config) { Nodens.config.getBoolean("debug") }

fun debug(vararg message: Any?) {
    if (debug) consoleMessage(*message.map { it.toString() }.toTypedArray())
}

fun consoleMessage(vararg message: String) {
    message.forEach {
        console().sendMessage("[Nodens] $it".colored())
    }
}