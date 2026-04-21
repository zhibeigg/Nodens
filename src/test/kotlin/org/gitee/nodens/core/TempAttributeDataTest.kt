package org.gitee.nodens.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TempAttributeDataTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  closed
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `永久属性 duration=-1 永远不会关闭`() {
        val data = TempAttributeData(-1L, emptyList())
        assertFalse(data.closed)
        // 即使等一小段时间也不会关闭
        Thread.sleep(10)
        assertFalse(data.closed)
    }

    @Test
    fun `有限时长属性刚创建时未关闭`() {
        val data = TempAttributeData(1000L, emptyList())
        assertFalse(data.closed)
    }

    @Test
    fun `有限时长属性过期后关闭`() {
        val data = TempAttributeData(10L, emptyList())
        Thread.sleep(20)
        assertTrue(data.closed)
    }

    @Test
    fun `duration=0 立即关闭`() {
        val data = TempAttributeData(0L, emptyList())
        assertTrue(data.closed)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  timeStampOver
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `timeStampOver 刚创建时接近0`() {
        val data = TempAttributeData(1000L, emptyList())
        assertTrue(data.timeStampOver < 50, "刚创建的 timeStampOver 应接近 0，实际: ${data.timeStampOver}")
    }

    @Test
    fun `timeStampOver 随时间增长`() {
        val data = TempAttributeData(1000L, emptyList())
        Thread.sleep(50)
        assertTrue(data.timeStampOver >= 40, "等待 50ms 后 timeStampOver 应 >= 40，实际: ${data.timeStampOver}")
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  timeStampClose
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `timeStampClose 等于创建时间加 duration`() {
        val before = System.currentTimeMillis()
        val data = TempAttributeData(5000L, emptyList())
        val after = System.currentTimeMillis()
        // timeStampClose 应在 [before+5000, after+5000] 范围内
        assertTrue(data.timeStampClose in (before + 5000)..(after + 5000))
    }

    @Test
    fun `永久属性的 timeStampClose 为创建时间减1`() {
        val before = System.currentTimeMillis()
        val data = TempAttributeData(-1L, emptyList())
        // duration=-1 时，timeStampClose = timestamp + (-1) = timestamp - 1
        assertTrue(data.timeStampClose < before)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  countdown
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `countdown 刚创建时接近 duration`() {
        val data = TempAttributeData(5000L, emptyList())
        assertTrue(data.countdown in 4900..5000, "刚创建的 countdown 应接近 5000，实际: ${data.countdown}")
    }

    @Test
    fun `countdown 过期后为0`() {
        val data = TempAttributeData(10L, emptyList())
        Thread.sleep(20)
        assertEquals(0L, data.countdown)
    }

    @Test
    fun `countdown 不会为负数`() {
        val data = TempAttributeData(1L, emptyList())
        Thread.sleep(10)
        assertTrue(data.countdown >= 0, "countdown 不应为负数")
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  构造参数
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `deathRemove 默认为 true`() {
        val data = TempAttributeData(1000L, emptyList())
        assertTrue(data.deathRemove)
    }

    @Test
    fun `deathRemove 可设为 false`() {
        val data = TempAttributeData(1000L, emptyList(), deathRemove = false)
        assertFalse(data.deathRemove)
    }

    @Test
    fun `attributeData 保持引用`() {
        val list = emptyList<IAttributeData>()
        val data = TempAttributeData(1000L, list)
        assertSame(list, data.attributeData)
    }
}
