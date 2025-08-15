package org.gitee.nodens.module.item.group

import org.gitee.nodens.core.reload.Reload
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

object GroupManager {

    @Config("group.yml")
    private lateinit var group: ConfigFile

    val itemGroups = hashMapOf<String, GroupCheck>()

    @Reload(0)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        group.reload()
        itemGroups.clear()
        group.getKeys(false).forEach { key ->
            val configurationSection = group.getConfigurationSection(key)!!
            itemGroups[key] = GroupCheck(key, configurationSection)
        }
    }
}