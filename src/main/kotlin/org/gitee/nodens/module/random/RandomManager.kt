package org.gitee.nodens.module.random

import kotlin.random.Random

object RandomManager {

    val random = Random(1).nextDouble(1.0, 100.0)

}