package org.gitee.nodens.module.item

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ContextSerializerTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            // 初始化 VariableRegistry.json（内置类型即可）
            VariableRegistry.rebuild()
        }
    }

    private fun roundTrip(context: NormalContext): NormalContext {
        val bytes = ContextSerializer.serialize(context)
        return ContextSerializer.deserialize(bytes)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  基本类型往返
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `roundtrip empty context`() {
        val ctx = NormalContext("test_key", hashMapOf(), 42)
        val result = roundTrip(ctx)
        assertEquals("test_key", result.key)
        assertEquals(42, result.hashcode)
        assertTrue(result.sourceMap().isEmpty())
    }

    @Test
    fun `roundtrip null variable`() {
        val ctx = NormalContext("k", hashMapOf("n" to NullVariable(null)), 0)
        val result = roundTrip(ctx)
        assertNull(result.sourceMap()["n"]?.value)
    }

    @Test
    fun `roundtrip byte variable`() {
        val ctx = NormalContext("k", hashMapOf("b" to ByteVariable(127)), 0)
        val result = roundTrip(ctx)
        assertEquals(127.toByte(), (result.sourceMap()["b"] as ByteVariable).value)
    }

    @Test
    fun `roundtrip short variable`() {
        val ctx = NormalContext("k", hashMapOf("s" to ShortVariable(32000)), 0)
        val result = roundTrip(ctx)
        assertEquals(32000.toShort(), (result.sourceMap()["s"] as ShortVariable).value)
    }

    @Test
    fun `roundtrip int variable`() {
        val ctx = NormalContext("k", hashMapOf("i" to IntVariable(999999)), 0)
        val result = roundTrip(ctx)
        assertEquals(999999, (result.sourceMap()["i"] as IntVariable).value)
    }

    @Test
    fun `roundtrip long variable`() {
        val ctx = NormalContext("k", hashMapOf("l" to LongVariable(Long.MAX_VALUE)), 0)
        val result = roundTrip(ctx)
        assertEquals(Long.MAX_VALUE, (result.sourceMap()["l"] as LongVariable).value)
    }

    @Test
    fun `roundtrip float variable`() {
        val ctx = NormalContext("k", hashMapOf("f" to FloatVariable(3.14f)), 0)
        val result = roundTrip(ctx)
        assertEquals(3.14f, (result.sourceMap()["f"] as FloatVariable).value)
    }

    @Test
    fun `roundtrip double variable`() {
        val ctx = NormalContext("k", hashMapOf("d" to DoubleVariable(2.718281828)), 0)
        val result = roundTrip(ctx)
        assertEquals(2.718281828, (result.sourceMap()["d"] as DoubleVariable).value)
    }

    @Test
    fun `roundtrip char variable`() {
        val ctx = NormalContext("k", hashMapOf("c" to CharVariable('Z')), 0)
        val result = roundTrip(ctx)
        assertEquals('Z', (result.sourceMap()["c"] as CharVariable).value)
    }

    @Test
    fun `roundtrip string variable`() {
        val ctx = NormalContext("k", hashMapOf("s" to StringVariable("hello世界")), 0)
        val result = roundTrip(ctx)
        assertEquals("hello世界", (result.sourceMap()["s"] as StringVariable).value)
    }

    @Test
    fun `roundtrip boolean variable`() {
        val ctx = NormalContext("k", hashMapOf("t" to BooleanVariable(true), "f" to BooleanVariable(false)), 0)
        val result = roundTrip(ctx)
        assertTrue((result.sourceMap()["t"] as BooleanVariable).value)
        assertFalse((result.sourceMap()["f"] as BooleanVariable).value)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  复合类型
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `roundtrip array variable`() {
        val arr = ArrayVariable(listOf(IntVariable(1), StringVariable("two"), BooleanVariable(true)))
        val ctx = NormalContext("k", hashMapOf("arr" to arr), 0)
        val result = roundTrip(ctx)
        val restored = result.sourceMap()["arr"] as ArrayVariable
        assertEquals(3, restored.value.size)
        assertEquals(1, (restored.value[0] as IntVariable).value)
        assertEquals("two", (restored.value[1] as StringVariable).value)
        assertTrue((restored.value[2] as BooleanVariable).value)
    }

    @Test
    fun `roundtrip map variable`() {
        val inner = MapVariable(mapOf("x" to IntVariable(10), "y" to DoubleVariable(2.5)))
        val ctx = NormalContext("k", hashMapOf("map" to inner), 0)
        val result = roundTrip(ctx)
        val restored = result.sourceMap()["map"] as MapVariable
        assertEquals(10, (restored.value["x"] as IntVariable).value)
        assertEquals(2.5, (restored.value["y"] as DoubleVariable).value)
    }

    @Test
    fun `roundtrip nested array in map`() {
        val nested = MapVariable(mapOf(
            "list" to ArrayVariable(listOf(IntVariable(1), IntVariable(2))),
            "name" to StringVariable("test")
        ))
        val ctx = NormalContext("k", hashMapOf("data" to nested), 0)
        val result = roundTrip(ctx)
        val restored = result.sourceMap()["data"] as MapVariable
        val list = restored.value["list"] as ArrayVariable
        assertEquals(2, list.value.size)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  多变量上下文
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `roundtrip context with multiple variables`() {
        val vars = hashMapOf<String, Variable<*>>(
            "name" to StringVariable("sword"),
            "damage" to IntVariable(100),
            "rate" to DoubleVariable(0.15),
            "enchanted" to BooleanVariable(true),
            "tags" to ArrayVariable(listOf(StringVariable("fire"), StringVariable("rare")))
        )
        val ctx = NormalContext("weapon_01", vars, 12345)
        val result = roundTrip(ctx)
        assertEquals("weapon_01", result.key)
        assertEquals(12345, result.hashcode)
        assertEquals(5, result.sourceMap().size)
        assertEquals("sword", (result.sourceMap()["name"] as StringVariable).value)
        assertEquals(100, (result.sourceMap()["damage"] as IntVariable).value)
    }
}
