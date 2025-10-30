package org.gitee.nodens

import org.gitee.nodens.api.Nodens
import org.gitee.nodens.api.NodensAPI
import org.gitee.nodens.util.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.disablePlugin
import taboolib.common.platform.function.pluginVersion
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
        println()
        consoleMessage(
            "§9    /|    / /",
            "§9   //|   / /      ___        ___   /      ___         __        ___",
            "§9  // |  / /     //   ) )   //   ) /     //___) )   //   ) )   ((   ) )  §8Nodens §eversion§7: §e$pluginVersion",
            "§9 //  | / /     //   / /   //   / /     //         //   / /     \\ \\    §7by. §bzhibei",
            "§9//   |/ /     ((___/ /   ((___/ /     ((____     //   / /   //   ) )"
        )
        println()
    }
}