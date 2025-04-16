package org.gitee.nodens

import org.gitee.nodens.api.Nodens
import org.gitee.nodens.api.NodensAPI
import taboolib.common.LifeCycle
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.disablePlugin
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerLifeCycleTask

object NodensPlugin : Plugin() {

    init {
        registerLifeCycleTask(LifeCycle.INIT) {
            try {
                Nodens.register(NodensAPI())
            } catch (ex: Throwable) {
                ex.printStackTrace()
                disablePlugin()
            }
        }
    }

    override fun onEnable() {
        info("Successfully running Nodens!")
    }
}