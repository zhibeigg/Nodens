package org.gitee.nodens.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PRDAlgorithmExtendedTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  quickGetC — 缓存查询
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `quickGetC 越界参数应抛出 IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            PRDAlgorithm.quickGetC(0.6)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PRDAlgorithm.quickGetC(-0.1)
        }
    }

    @Test
    fun `quickGetC 边界值 0 应抛出异常 - 缓存中无 0 的 key`() {
        // ptoC(0.0) 不在缓存中（initCache 从 0.01 开始），所以 quickGetC(0.0) 应抛出
        assertThrows(Exception::class.java) {
            PRDAlgorithm.quickGetC(0.0)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  ctoP 边界情况
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `ctoP 返回正数`() {
        // 对于合理的 C 值范围，ctoP 应返回正数
        for (c in listOf(0.01, 0.05, 0.10, 0.20, 0.30)) {
            val p = PRDAlgorithm.ctoP(c)
            assertTrue(p > 0.0, "ctoP($c) = $p 应 > 0")
            assertTrue(p.isFinite(), "ctoP($c) = $p 应为有限值")
        }
    }

    @Test
    fun `ctoP 在低 C 值范围内单调递增`() {
        // PRD 算法的 ctoP 在高 C 值区域（> 0.25）由于 maxTries 离散化会出现非单调行为
        // 这里只验证有效工作范围（C <= 0.25）内的单调性
        var prevP = 0.0
        for (c in listOf(0.01, 0.05, 0.10, 0.15, 0.20, 0.25)) {
            val p = PRDAlgorithm.ctoP(c)
            assertTrue(p > prevP, "ctoP 应单调递增: ctoP($c)=$p <= $prevP")
            prevP = p
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  ptoC 边界情况
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `ptoC 边界值 0_5`() {
        val c = PRDAlgorithm.ptoC(0.5)
        assertTrue(c > 0.0 && c <= 0.5, "ptoC(0.5) = $c 应在 (0, 0.5] 范围内")
    }

    @Test
    fun `ptoC 极小概率`() {
        val c = PRDAlgorithm.ptoC(0.01)
        assertTrue(c > 0.0, "ptoC(0.01) 应 > 0")
        assertTrue(c <= 0.01, "ptoC(0.01) = $c 应 <= 0.01")
    }

    @Test
    fun `ptoC 精度验证 - 有效范围内的概率值`() {
        // PRD 算法在 0-0.5 范围内有效，使用合理的 C 值范围
        val testPs = listOf(0.01, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40)
        for (p in testPs) {
            val c = PRDAlgorithm.ptoC(p)
            val recovered = PRDAlgorithm.ctoP(c)
            assertTrue(
                kotlin.math.abs(recovered - p) < 0.02,
                "p=$p → c=$c → recovered=$recovered, 误差过大"
            )
        }
    }

    @Test
    fun `ptoC 返回值始终小于等于输入概率`() {
        for (p in listOf(0.01, 0.05, 0.10, 0.20, 0.30, 0.40, 0.50)) {
            val c = PRDAlgorithm.ptoC(p)
            assertTrue(c <= p, "ptoC($p) = $c 应 <= $p")
        }
    }

    @Test
    fun `ptoC 返回值始终为正数`() {
        for (p in listOf(0.01, 0.05, 0.10, 0.20, 0.30, 0.40, 0.50)) {
            val c = PRDAlgorithm.ptoC(p)
            assertTrue(c > 0.0, "ptoC($p) = $c 应 > 0")
        }
    }
}
