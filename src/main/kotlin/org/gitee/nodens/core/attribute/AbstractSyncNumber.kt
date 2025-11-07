package org.gitee.nodens.core.attribute

import org.gitee.nodens.util.MonitorLazy

abstract class AbstractSyncNumber: AbstractNumber(), ISyncDefault {

    override val default by MonitorLazy({ config }) { config.getDouble("default", 20.0) }
}