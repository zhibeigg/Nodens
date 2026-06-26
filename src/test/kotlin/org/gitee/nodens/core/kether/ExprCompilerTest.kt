package org.gitee.nodens.core.kether

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ExprCompilerTest {

    private fun eval(src: String, vars: Map<String, Double> = emptyMap()): Double =
        ExprCompiler.compile(src).eval { vars[it] ?: 0.0 }

    @Test
    fun `常量与四则`() {
        assertEquals(3.0, eval("1+2"), 1e-9)
        assertEquals(7.0, eval("1+2*3"), 1e-9)
        assertEquals(9.0, eval("(1+2)*3"), 1e-9)
        assertEquals(2.0, eval("8/2/2"), 1e-9)
        assertEquals(1.0, eval("10%3"), 1e-9)
        assertEquals(2.5, eval("5/2"), 1e-9)
    }

    @Test
    fun `优先级与左结合`() {
        assertEquals(-1.0, eval("2-3"), 1e-9)
        assertEquals(0.0, eval("2-1-1"), 1e-9)
        assertEquals(14.0, eval("2+3*4"), 1e-9)
        assertEquals(20.0, eval("(2+3)*4"), 1e-9)
    }

    @Test
    fun `一元负号`() {
        assertEquals(-5.0, eval("-5"), 1e-9)
        assertEquals(-1.0, eval("-(2-1)"), 1e-9)
        assertEquals(5.0, eval("3--2"), 1e-9)
        assertEquals(-6.0, eval("-2*3"), 1e-9)
        assertEquals(1.0, eval("2+-1"), 1e-9)
    }

    @Test
    fun `变量与缺失变量为0`() {
        assertEquals(7.0, eval("a+b", mapOf("a" to 3.0, "b" to 4.0)), 1e-9)
        assertEquals(3.0, eval("a+missing", mapOf("a" to 3.0)), 1e-9)
        assertEquals(0.0, eval("nope"), 1e-9)
    }

    @Test
    fun `内置函数 min max clamp`() {
        assertEquals(2.0, eval("min(2,5)"), 1e-9)
        assertEquals(5.0, eval("max(2,5)"), 1e-9)
        assertEquals(1.3, eval("clamp(2, 0.7, 1.3)"), 1e-9)
        assertEquals(0.7, eval("clamp(0.1, 0.7, 1.3)"), 1e-9)
        assertEquals(1.0, eval("clamp(1, 0.7, 1.3)"), 1e-9)
        assertEquals(0.0, eval("clamp(reduction, 0, 1)", mapOf("reduction" to -3.0)), 1e-9)
        assertEquals(1.0, eval("clamp(reduction, 0, 1)", mapOf("reduction" to 9.0)), 1e-9)
    }

    @Test
    fun `内置函数 round floor ceil abs pct`() {
        assertEquals(3.0, eval("round(2.5)"), 1e-9)
        assertEquals(2.0, eval("round(2.4)"), 1e-9)
        assertEquals(2.0, eval("floor(2.9)"), 1e-9)
        assertEquals(3.0, eval("ceil(2.1)"), 1e-9)
        assertEquals(4.0, eval("abs(-4)"), 1e-9)
        assertEquals(0.1, eval("pct(10)"), 1e-9)
        assertEquals(0.85, eval("1 - pct(15)"), 1e-9)
    }

    @Test
    fun `嵌套函数与表达式`() {
        assertEquals(1.3, eval("max(0.7, min(1.3, x))", mapOf("x" to 5.0)), 1e-9)
        assertEquals(0.7, eval("clamp(1 + (a - d) * 0.02, 0.7, 1.3)", mapOf("a" to 0.0, "d" to 100.0)), 1e-9)
    }

    @Test
    fun `真实伤害末段公式`() {
        // ((damage+addon)*(1-defence/(1000+defence))*jjk*ehMul*redMul + real*ehMul)*scale + critAddon*scale
        val vars = mapOf(
            "damage" to 500.0, "addon" to 100.0, "defence" to 1000.0,
            "jjk" to 1.0, "ehMul" to 1.2, "redMul" to 0.9,
            "real" to 50.0, "scale" to 1.0, "critAddon" to 30.0
        )
        val expr = "((damage+addon)*(1-defence/(1000+defence))*jjk*ehMul*redMul + real*ehMul)*scale + critAddon*scale"
        // (600 * (1 - 1000/2000) * 1 * 1.2 * 0.9 + 50*1.2) * 1 + 30*1 = (600*0.5*1.08 + 60) + 30 = 324 + 60 + 30 = 414
        assertEquals(414.0, eval(expr, vars), 1e-9)
    }

    @Test
    fun `scale 缩放生效`() {
        val v = mapOf("a" to 10.0, "scale" to 2.5)
        assertEquals(25.0, eval("a*scale", v), 1e-9)
    }

    @Test
    fun `语法错误抛异常`() {
        assertThrows(IllegalStateException::class.java) { ExprCompiler.compile("") }
        assertThrows(IllegalStateException::class.java) { ExprCompiler.compile("(1+2") }
        assertThrows(IllegalStateException::class.java) { ExprCompiler.compile("1+2)") }
        assertThrows(IllegalStateException::class.java) { ExprCompiler.compile("clamp(1,2)") }   // 参数过少
        assertThrows(IllegalStateException::class.java) { ExprCompiler.compile("min(1)") }       // 参数过少
        assertThrows(IllegalStateException::class.java) { ExprCompiler.compile("1 2") }          // 缺运算符
        assertThrows(IllegalStateException::class.java) { ExprCompiler.compile("*5") }           // 缺左操作数
        assertThrows(IllegalStateException::class.java) { ExprCompiler.compile("nope(1,2)") }    // 未知函数
    }

    @Test
    fun `variableNames 去重有序`() {
        val c = ExprCompiler.compile("a + b * a - c")
        assertEquals(listOf("a", "b", "c"), c.variableNames)
    }
}
