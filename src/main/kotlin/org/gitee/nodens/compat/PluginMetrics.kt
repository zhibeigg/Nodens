package org.gitee.nodens.compat

import org.gitee.nodens.core.AttributeManager
import taboolib.common.LifeCycle
import taboolib.common.platform.Platform
import taboolib.module.metrics.Metrics
import taboolib.module.metrics.charts.SingleLineChart
import taboolib.platform.BukkitPlugin
import taboolib.platform.bukkit.Parallel

object PluginMetrics {

    internal lateinit var metrics: Metrics
        private set

    @Parallel(runOn = LifeCycle.ENABLE)
    private fun init() {
        metrics = Metrics(25468, BukkitPlugin.getInstance().description.version, Platform.BUKKIT)
        metrics.addCustomChart(SingleLineChart("attributes") {
            AttributeManager.ATTRIBUTE_MATCHING_MAP.size
        })
    }
}