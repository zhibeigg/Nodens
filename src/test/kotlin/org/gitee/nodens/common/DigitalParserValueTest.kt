package org.gitee.nodens.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * DigitalParser.Value 和 DigitalParser.Type 的纯数据结构测试
 *
 * 注意: DigitalParser.getValue() 依赖 taboolib.common5.cdouble 和 IAttributeGroup.Number，
 * 无法在纯测试环境中测试。这里只测试 Value 数据类的行为。
 */
class DigitalParserValueTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  Value.isEmpty
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `isEmpty 全零 SINGLE 返回 true`() {
        val v = DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf(0.0))
        assertTrue(v.isEmpty())
    }

    @Test
    fun `isEmpty 全零 RANGE 返回 true`() {
        val v = DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf(0.0, 0.0))
        assertTrue(v.isEmpty())
    }

    @Test
    fun `isEmpty 非零返回 false`() {
        val v = DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf(0.0, 1.0))
        assertFalse(v.isEmpty())
    }

    @Test
    fun `isEmpty 空数组返回 true`() {
        val v = DigitalParser.Value(DigitalParser.Type.PERCENT, doubleArrayOf())
        assertTrue(v.isEmpty())
    }

    @Test
    fun `isEmpty 负数返回 false`() {
        val v = DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf(-1.0))
        assertFalse(v.isEmpty())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  Value 数据持有
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `Value 持有 type 和 doubleArray`() {
        val arr = doubleArrayOf(10.0, 20.0)
        val v = DigitalParser.Value(DigitalParser.Type.PERCENT, arr)
        assertEquals(DigitalParser.Type.PERCENT, v.type)
        assertSame(arr, v.doubleArray)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  Type 枚举
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `Type 枚举包含 PERCENT 和 COUNT`() {
        val types = DigitalParser.Type.entries
        assertEquals(2, types.size)
        assertTrue(types.contains(DigitalParser.Type.PERCENT))
        assertTrue(types.contains(DigitalParser.Type.COUNT))
    }
}
