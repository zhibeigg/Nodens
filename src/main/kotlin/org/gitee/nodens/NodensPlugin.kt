package org.gitee.nodens

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import javax.security.auth.login.Configuration

object NodensPlugin : Plugin() {

    override fun onEnable() {
        info("Successfully running Nodens!")
    }
}