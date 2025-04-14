package org.gitee.nodens

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

object Nodens : Plugin() {

    override fun onEnable() {
        info("Successfully running Nodens!")
    }
}