package org.gitee.nodens.util

import org.gitee.nodens.api.Nodens
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.service.PlatformIO
import taboolib.module.chat.colored

var debug: Boolean = Nodens.config.getBoolean("debug")

fun debug(vararg message: Any?) {
    if (debug) PlatformFactory.getService<PlatformIO>().info(*message.map { "&6[debug] $it".colored() }.toTypedArray())
}