package org.gitee.nodens.util

import org.bukkit.Bukkit
import taboolib.common.platform.function.console
import taboolib.module.lang.sendLang

class Plugin(val name: String, val extensionFunction: () -> Unit = {}) {

    val isEnabled
        get() = Bukkit.getPluginManager().getPlugin(name) != null

    fun load() {
        if (isEnabled) {
            extensionFunction()
            console().sendLang("hook-true", name)
        } else {
            console().sendLang("hook-false", name)
        }
    }
}

val MythicMobsPlugin = Plugin("MythicMobs")

val GlowAPIPlugin = Plugin("GlowAPI")

val RedisChannelPlugin = Plugin("RedisChannel")

val DragonCorePlugin = Plugin("DragonCore")