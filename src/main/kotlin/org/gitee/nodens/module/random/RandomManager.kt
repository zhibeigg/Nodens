package org.gitee.nodens.module.random

import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.util.files
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Configuration

object RandomManager {

    val randomsMap = hashMapOf<String, Randoms>()

    class Randoms(val id: String, val action: String)

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        randomsMap.clear()
        try {
            files("randoms", "example.yml") {
                val config = Configuration.loadFromFile(it)
                config.getKeys(false).forEach { id ->
                    randomsMap[id] = Randoms(id, config.getString(id) ?: error("Missing $id Action"))
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}