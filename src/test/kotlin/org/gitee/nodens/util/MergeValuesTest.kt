package org.gitee.nodens.util

import org.gitee.nodens.common.DigitalParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MergeValuesTest {

    private fun value(type: DigitalParser.Type, vararg values: Double): DigitalParser.Value {
        return DigitalParser.Value(type, values)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  mergeValues (vararg)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `mergeValues 空输入返回空 map`() {
        val result = mergeValues()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `mergeValues 单个值直接返回`() {
        val result = mergeValues(value(DigitalParser.Type.COUNT, 10.0, 20.0))
        assertEquals(1, result.size)
        assertArrayEquals(doubleArrayOf(10.0, 20.0), result[DigitalParser.Type.COUNT])
    }

    @Test
    fun `mergeValues 同类型累加`() {
        val result = mergeValues(
            value(DigitalParser.Type.COUNT, 10.0, 20.0),
            value(DigitalParser.Type.COUNT, 5.0, 15.0)
        )
        assertEquals(1, result.size)
        assertArrayEquals(doubleArrayOf(15.0, 35.0), result[DigitalParser.Type.COUNT])
    }

    @Test
    fun `mergeValues 不同类型分组`() {
        val result = mergeValues(
            value(DigitalParser.Type.COUNT, 10.0),
            value(DigitalParser.Type.PERCENT, 0.5)
        )
        assertEquals(2, result.size)
        assertArrayEquals(doubleArrayOf(10.0), result[DigitalParser.Type.COUNT])
        assertArrayEquals(doubleArrayOf(0.5), result[DigitalParser.Type.PERCENT])
    }

    @Test
    fun `mergeValues 不同长度数组合并 - 新数组更长`() {
        val result = mergeValues(
            value(DigitalParser.Type.COUNT, 10.0),
            value(DigitalParser.Type.COUNT, 5.0, 20.0)
        )
        // 第一个 [10.0] + 第二个 [5.0, 20.0] → [15.0, 20.0]
        assertArrayEquals(doubleArrayOf(15.0, 20.0), result[DigitalParser.Type.COUNT])
    }

    @Test
    fun `mergeValues 不同长度数组合并 - 已有数组更长`() {
        val result = mergeValues(
            value(DigitalParser.Type.COUNT, 10.0, 20.0),
            value(DigitalParser.Type.COUNT, 5.0)
        )
        // 第一个 [10.0, 20.0] + 第二个 [5.0] → [15.0, 20.0]
        assertArrayEquals(doubleArrayOf(15.0, 20.0), result[DigitalParser.Type.COUNT])
    }

    @Test
    fun `mergeValues 多个值混合类型`() {
        val result = mergeValues(
            value(DigitalParser.Type.COUNT, 10.0, 20.0),
            value(DigitalParser.Type.PERCENT, 0.1, 0.2),
            value(DigitalParser.Type.COUNT, 5.0, 10.0),
            value(DigitalParser.Type.PERCENT, 0.05, 0.1)
        )
        assertArrayEquals(doubleArrayOf(15.0, 30.0), result[DigitalParser.Type.COUNT])
        assertArrayEquals(doubleArrayOf(0.15, 0.30), result[DigitalParser.Type.PERCENT], 1e-10)
    }

    @Test
    fun `mergeValues 不修改原始数组`() {
        val original = doubleArrayOf(10.0, 20.0)
        val v = DigitalParser.Value(DigitalParser.Type.COUNT, original)
        mergeValues(v, value(DigitalParser.Type.COUNT, 5.0, 5.0))
        // 原始数组不应被修改
        assertArrayEquals(doubleArrayOf(10.0, 20.0), original)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  mergeValues (List)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `mergeValues List 版本空列表`() {
        val result = mergeValues(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `mergeValues List 版本正常合并`() {
        val result = mergeValues(listOf(
            value(DigitalParser.Type.COUNT, 10.0),
            value(DigitalParser.Type.COUNT, 5.0)
        ))
        assertArrayEquals(doubleArrayOf(15.0), result[DigitalParser.Type.COUNT])
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  mergeMaps
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `mergeMaps 两个 null 返回空 map`() {
        val result = mergeMaps(null, null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `mergeMaps 第一个 null 返回第二个`() {
        val map2 = mapOf(DigitalParser.Type.COUNT to doubleArrayOf(10.0))
        val result = mergeMaps(null, map2)
        assertSame(map2, result)
    }

    @Test
    fun `mergeMaps 第二个 null 返回第一个`() {
        val map1 = mapOf(DigitalParser.Type.COUNT to doubleArrayOf(10.0))
        val result = mergeMaps(map1, null)
        assertSame(map1, result)
    }

    @Test
    fun `mergeMaps 第一个空 map 返回第二个`() {
        val map2 = mapOf(DigitalParser.Type.COUNT to doubleArrayOf(10.0))
        val result = mergeMaps(emptyMap(), map2)
        assertSame(map2, result)
    }

    @Test
    fun `mergeMaps 同类型累加`() {
        val map1 = mapOf(DigitalParser.Type.COUNT to doubleArrayOf(10.0, 20.0))
        val map2 = mapOf(DigitalParser.Type.COUNT to doubleArrayOf(5.0, 10.0))
        val result = mergeMaps(map1, map2)
        assertArrayEquals(doubleArrayOf(15.0, 30.0), result[DigitalParser.Type.COUNT])
    }

    @Test
    fun `mergeMaps 不同类型合并`() {
        val map1 = mapOf(DigitalParser.Type.COUNT to doubleArrayOf(10.0))
        val map2 = mapOf(DigitalParser.Type.PERCENT to doubleArrayOf(0.5))
        val result = mergeMaps(map1, map2)
        assertEquals(2, result.size)
        assertArrayEquals(doubleArrayOf(10.0), result[DigitalParser.Type.COUNT])
        assertArrayEquals(doubleArrayOf(0.5), result[DigitalParser.Type.PERCENT])
    }

    @Test
    fun `mergeMaps 混合类型`() {
        val map1 = mapOf(
            DigitalParser.Type.COUNT to doubleArrayOf(10.0),
            DigitalParser.Type.PERCENT to doubleArrayOf(0.1)
        )
        val map2 = mapOf(
            DigitalParser.Type.COUNT to doubleArrayOf(5.0),
            DigitalParser.Type.PERCENT to doubleArrayOf(0.2)
        )
        val result = mergeMaps(map1, map2)
        assertArrayEquals(doubleArrayOf(15.0), result[DigitalParser.Type.COUNT])
        assertArrayEquals(doubleArrayOf(0.3), result[DigitalParser.Type.PERCENT], 1e-10)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  DigitalParser.Value.isEmpty
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `Value isEmpty 全零返回 true`() {
        assertTrue(value(DigitalParser.Type.COUNT, 0.0, 0.0).isEmpty())
    }

    @Test
    fun `Value isEmpty 非零返回 false`() {
        assertFalse(value(DigitalParser.Type.COUNT, 0.0, 1.0).isEmpty())
    }

    @Test
    fun `Value isEmpty 空数组返回 true`() {
        assertTrue(DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf()).isEmpty())
    }
}
