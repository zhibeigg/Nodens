package org.gitee.nodens.core.attribute

import org.gitee.nodens.util.MonitorLazy

abstract class AbstractSyncPercentNumber: AbstractPercentNumber(), ISyncDefault {

    override val default by MonitorLazy({ config }) { config.getDouble("default", 0.2) }
}