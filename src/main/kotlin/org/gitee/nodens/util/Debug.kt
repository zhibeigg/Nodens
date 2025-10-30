package org.gitee.nodens.util

import org.gitee.nodens.api.Nodens
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.console
import taboolib.common.platform.service.PlatformIO
import taboolib.module.chat.colored

val debug: Boolean by ConfigLazy(Nodens.config) { getBoolean("debug") }

fun debug(vararg message: Any?) {
    if (debug) PlatformFactory.getService<PlatformIO>().warning(*message.map { "[debug] $it" }.toTypedArray())
}

fun consoleMessage(vararg message: String) {
    message.forEach {
        console().sendMessage("[Orryx] $it")
    }
}