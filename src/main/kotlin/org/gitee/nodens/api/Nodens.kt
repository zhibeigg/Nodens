package org.gitee.nodens.api

import org.gitee.nodens.common.Handle.handle
import org.gitee.nodens.core.reload.Reload
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

object Nodens {

    @Config
    lateinit var config: ConfigFile
        private set

    @Reload(0)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        config.reload()
        handle.reload()
        info("&e┣&7Config loaded &a√".colored())
    }

}