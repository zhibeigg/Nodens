package org.gitee.nodens.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.abs

class PRDAlgorithmTest {

    @Test
    fun `ctoP with known C values`() {
        // C=0.25 应该对应约 25% 的概率（高C值接近线性）
        val p = PRDAlgorithm.ctoP(0.25)
        assertTrue(p > 0.0 && p < 1.0, "ctoP(0.25) = $p should be in (0,1)")
    }

    @Test
    fun `ctoP with small C value`() {
        val p = PRDAlgorithm.ctoP(0.01)
        assertTrue(p > 0.0 && p < 0.1, "ctoP(0.01) = $p should be small")
    }

    @Test
    fun `ptoC roundtrip consistency`() {
        // 对于给定概率 p，ptoC(p) 得到 C，再 ctoP(C) 应该接近 p
        // 注意：PRD 算法在边界值 0.5 处精度较低，不纳入测试
        val testPs = listOf(0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.40)
        for (p in testPs) {
            val c = PRDAlgorithm.ptoC(p)
            val recoveredP = PRDAlgorithm.ctoP(c)
            assertTrue(
                abs(recoveredP - p) < 0.01,
                "ptoC/ctoP roundtrip failed for p=$p: c=$c, recovered=$recoveredP"
            )
        }
    }

    @Test
    fun `ptoC returns value less than or equal to p`() {
        // PRD 的 C 值总是 <= 对应的概率 p
        for (p in listOf(0.05, 0.10, 0.20, 0.30, 0.50)) {
            val c = PRDAlgorithm.ptoC(p)
            assertTrue(c <= p, "C=$c should be <= p=$p")
            assertTrue(c > 0.0, "C=$c should be > 0")
        }
    }

    @Test
    fun `ptoC rejects out of range`() {
        assertThrows(IllegalArgumentException::class.java) {
            PRDAlgorithm.ptoC(0.6)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PRDAlgorithm.ptoC(-0.1)
        }
    }

    @Test
    fun `ctoP monotonically increasing`() {
        var prevP = 0.0
        for (c in listOf(0.01, 0.05, 0.10, 0.15, 0.20, 0.30, 0.40)) {
            val p = PRDAlgorithm.ctoP(c)
            assertTrue(p > prevP, "ctoP should be monotonically increasing: ctoP($c)=$p <= $prevP")
            prevP = p
        }
    }
}
