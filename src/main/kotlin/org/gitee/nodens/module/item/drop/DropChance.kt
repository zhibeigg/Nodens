package org.gitee.nodens.module.item.drop

import org.gitee.nodens.common.PRDAlgorithm
import taboolib.common.util.random
import java.math.BigDecimal
import java.math.RoundingMode

class DropChance(percent: Double) {

    val c = PRDAlgorithm.quickGetC(BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP).toDouble())
    var times = 0

    fun hasDrop(): Boolean {
        times ++
        if (random(c * times)) {
            times = 0
            return true
        } else {
            return false
        }
    }
}