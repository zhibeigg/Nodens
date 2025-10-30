package org.gitee.nodens.core.database

import org.bukkit.entity.Player
import org.gitee.nodens.core.reload.Reload
import taboolib.common.io.newFile
import taboolib.common.platform.function.getDataFolder
import taboolib.common5.cint
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import java.util.concurrent.CompletableFuture

class JSONManager: ISyncCache {

    private var cache: Configuration? = null

    companion object {

        @Reload(1)
        private fun reload() {
            val instance = ISyncCache.INSTANCE
            if (instance is JSONManager) {
                instance.reload()
            }
        }
    }

    override fun getDropTimes(player: Player, key: String, global: Boolean): CompletableFuture<Int> {
        return CompletableFuture.completedFuture(loadFile(player, global)[key].cint)
    }

    override fun setDropTimes(player: Player, key: String, times: Int, global: Boolean) {
        val config = loadFile(player, global)
        config[key] = times
        config.saveToFile()
    }

    fun loadFile(player: Player, global: Boolean): Configuration {
        return cache ?: run {
            if (global) {
                Configuration.loadFromFile(newFile(getDataFolder(), "/data/global.json"), Type.JSON, true)
            } else {
                Configuration.loadFromFile(newFile(getDataFolder(), "/data/${player.uniqueId}.json"), Type.JSON, true)
            }.apply {
                cache = this
            }
        }
    }

    fun reload() {
        cache = null
    }
}