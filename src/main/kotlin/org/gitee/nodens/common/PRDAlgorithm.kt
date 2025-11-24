package org.gitee.nodens.common

import kotlinx.serialization.json.Json
import org.gitee.nodens.util.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.getDataFolder
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.ceil

object PRDAlgorithm {

    private var cacheMap = hashMapOf<Double, Double>()

    @Awake(LifeCycle.LOAD)
    fun initCache() {
        val file = File(getDataFolder(), "PRDCache.json")
        if (!file.exists()) {
            consoleMessage("&e┣&7检测到PRD算法C值未缓存，开始计算并写入缓存文件......")
            var percent = 0.00
            while (percent < 0.5) {
                percent = BigDecimal.valueOf(percent + 0.01)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()
                val c = ptoC(percent)
                cacheMap[percent] = c
                consoleMessage("&e┣&7计算成功 Percent: $percent c=${c} &a√")
            }
            file.writeText(Json.encodeToString(cacheMap))
            consoleMessage("&e┣&7写入C值缓存文件成功 &a√")
        } else {
            cacheMap = Json.decodeFromString(file.readText())
            consoleMessage("&e┣&7检测到PRD算法C值缓存 加载成功 &a√")
        }
    }

    /**
     * 根据C值转概率值
     * */
    fun ctoP(c: Double): Double {
        var po: Double
        var pb = 0.0
        var sumN = 0.0
        val maxTries = ceil(1 / c).toInt()

        for (n in 0 until maxTries) {
            po = minOf(1.0, c * n) * (1 - pb)
            pb += po
            sumN += n * po
        }

        return 1 / sumN
    }

    /**
     * 根据概率推算C值
     * */
    fun ptoC(p: Double): Double {
        require(p in 0.0..0.5) { "PRD算法仅在0-0.5之间具备准确性" }
        var cu = p
        var cl = 0.0
        var p1: Double
        var p2 = 1.0

        while (true) {
            val cm = (cu + cl) / 2
            p1 = ctoP(cm)

            if (abs(p1 - p2) <= 1e-9) {
                break
            }

            if (p1 > p) {
                cu = cm
            } else {
                cl = cm
            }
            p2 = p1
        }

        return (cu + cl) / 2
    }

    fun quickGetC(p: Double): Double {
        require(p in 0.0..0.5) { "PRD算法仅在0-0.5之间具备准确性" }
        val key = BigDecimal.valueOf(p)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
        return cacheMap[key] ?: error("找不到 Percent${p} 对应的 C 值缓存")
    }
}