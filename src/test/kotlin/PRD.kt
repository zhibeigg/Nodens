import taboolib.common.util.random
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.system.measureTimeMillis

object PRD {

    @JvmStatic
    fun main(args: Array<String>) {
        println("输入一个概率")
        val chance = readLine()!!.toDouble()

        require(chance in 0.0..0.5) { "PRD算法仅在0-0.5之间具备准确性" }

        val c: Double

        val timeout = measureTimeMillis {
            c = cFromP(chance)
        }

        var now = c
        var success = 0
        var times = 0
        var min = 0
        var max = 0
        repeat(100_000) {
            times ++
            if (random(now.coerceAtMost(1.0))) {
                println("第${times}次尝试 -> 掉落成功 (真实概率: ${"%2f".format(now*100)}%) √")
                if (min == 0 || times < min) {
                    min = times
                }
                if (max == 0 || times > max) {
                    max = times
                }
                success ++
                now = c
                times = 0
            } else {
                println("第${times}次尝试 -> 掉落失败 (真实概率: ${"%2f".format(now*100)}%) ×")
                now += c
            }
        }
        println(success / 100_000.000)
        println("耗时 $timeout")
        println("运气最好的情况 $min 次")
        println("运气最坏的情况 $max 次")
    }

    fun pFromC(c: Double): Double {
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

    fun cFromP(p: Double): Double {
        var cu = p
        var cl = 0.0
        var p1: Double
        var p2 = 1.0

        while (true) {
            val cm = (cu + cl) / 2
            p1 = pFromC(cm)

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
}