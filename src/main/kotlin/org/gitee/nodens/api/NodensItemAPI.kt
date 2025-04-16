package org.gitee.nodens.api

import org.gitee.nodens.api.interfaces.IItemAPI
import org.gitee.nodens.module.item.IItemGenerator
import org.gitee.nodens.module.item.ItemConfig
import org.gitee.nodens.module.item.ItemManager
import org.gitee.nodens.module.item.generator.NormalGenerator
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

class NodensItemAPI: IItemAPI {

    override fun getItemConfig(key: String): ItemConfig? {
        return ItemManager.itemConfigs[key]
    }

    override fun getItemGenerator(): IItemGenerator {
        return PlatformFactory.getAPI<IItemGenerator>()
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<IItemAPI>(NodensItemAPI())
            PlatformFactory.registerAPI<IItemGenerator>(NormalGenerator)
        }
    }
}