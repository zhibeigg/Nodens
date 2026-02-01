package org.gitee.nodens.core.attribute

import org.gitee.nodens.api.Nodens
import taboolib.module.configuration.util.ReloadAwareLazy

abstract class AbstractSyncNumber: AbstractNumber(), ISyncDefault {

    override val default by ReloadAwareLazy(Nodens.config) { config.getDouble("default", 20.0) }
}