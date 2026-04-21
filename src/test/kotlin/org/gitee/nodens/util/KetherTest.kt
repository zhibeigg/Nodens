package org.gitee.nodens.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class KetherTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  getBytes 基础包装
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `普通脚本自动包装 def main`() {
        val result = String(getBytes("print hello"), StandardCharsets.UTF_8)
        assertEquals("def main = { print hello }", result)
    }

    @Test
    fun `以 def 开头的脚本不包装`() {
        val script = "def main = { print hello }"
        val result = String(getBytes(script), StandardCharsets.UTF_8)
        assertEquals(script, result)
    }

    @Test
    fun `def 开头但不是 def 空格 不包装`() {
        val script = "def myFunc = { 1 + 2 }"
        val result = String(getBytes(script), StandardCharsets.UTF_8)
        assertEquals(script, result)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  注释过滤
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `多行脚本中 # 注释行被过滤`() {
        // 注意: 包装后第一行变成 "def main = { # 这是注释"，trim 后不以 # 开头，所以保留
        // 独立的 # 注释行会被过滤
        val script = "print hello\n# 这是注释\nprint world"
        val result = String(getBytes(script), StandardCharsets.UTF_8)
        assertEquals("def main = { print hello\nprint world }", result)
    }

    @Test
    fun `返回 UTF-8 编码字节数组`() {
        val script = "print 你好"
        val bytes = getBytes(script)
        val result = String(bytes, StandardCharsets.UTF_8)
        assertTrue(result.contains("你好"))
    }
}
