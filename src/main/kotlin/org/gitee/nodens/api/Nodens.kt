package org.gitee.nodens.api

import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.api.interfaces.INodensAPI
import org.gitee.nodens.common.Handle.handle
import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.util.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
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
        consoleMessage("&e┣&7Config loaded &a√")
    }

    private var api: INodensAPI? = null

    /**
     * 注册开发者接口
     */
    fun register(api: INodensAPI) {
        this.api = api
    }

    /**
     * 获取开发者接口
     */
    fun api(): INodensAPI {
        return api ?: error("OrryxAPI has not finished loading, or failed to load!")
    }
}