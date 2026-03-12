package org.gitee.nodens.module.item

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VariablesTest {

    @Test
    fun `NullVariable value is null`() {
        val v = NullVariable(null)
        assertNull(v.value)
    }

    @Test
    fun `ByteVariable holds byte`() {
        val v = ByteVariable(42.toByte())
        assertEquals(42.toByte(), v.value)
    }

    @Test
    fun `ShortVariable holds short`() {
        val v = ShortVariable(1000.toShort())
        assertEquals(1000.toShort(), v.value)
    }

    @Test
    fun `IntVariable holds int`() {
        val v = IntVariable(123456)
        assertEquals(123456, v.value)
    }

    @Test
    fun `LongVariable holds long`() {
        val v = LongVariable(9876543210L)
        assertEquals(9876543210L, v.value)
    }

    @Test
    fun `FloatVariable holds float`() {
        val v = FloatVariable(3.14f)
        assertEquals(3.14f, v.value)
    }

    @Test
    fun `DoubleVariable holds double`() {
        val v = DoubleVariable(2.718281828)
        assertEquals(2.718281828, v.value)
    }

    @Test
    fun `CharVariable holds char`() {
        val v = CharVariable('A')
        assertEquals('A', v.value)
    }

    @Test
    fun `StringVariable holds string`() {
        val v = StringVariable("hello")
        assertEquals("hello", v.value)
    }

    @Test
    fun `BooleanVariable holds boolean`() {
        assertTrue(BooleanVariable(true).value)
        assertFalse(BooleanVariable(false).value)
    }

    @Test
    fun `ArrayVariable holds list of variables`() {
        val list = listOf(IntVariable(1), StringVariable("two"))
        val v = ArrayVariable(list)
        assertEquals(2, v.value.size)
        assertEquals(1, (v.value[0] as IntVariable).value)
        assertEquals("two", (v.value[1] as StringVariable).value)
    }

    @Test
    fun `MapVariable holds map of variables`() {
        val inner = mapOf("a" to IntVariable(10), "b" to StringVariable("x"))
        val v = MapVariable(inner)
        assertEquals(10, (v.value["a"] as IntVariable).value)
        assertEquals("x", (v.value["b"] as StringVariable).value)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  data class equals / hashCode
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `equals and hashCode for same values`() {
        assertEquals(IntVariable(42), IntVariable(42))
        assertEquals(IntVariable(42).hashCode(), IntVariable(42).hashCode())

        assertEquals(StringVariable("abc"), StringVariable("abc"))
        assertEquals(NullVariable(null), NullVariable(null))
    }

    @Test
    fun `not equals for different values`() {
        assertNotEquals(IntVariable(1), IntVariable(2))
        assertNotEquals(StringVariable("a"), StringVariable("b"))
    }

    @Test
    fun `nested ArrayVariable equals`() {
        val a = ArrayVariable(listOf(IntVariable(1), IntVariable(2)))
        val b = ArrayVariable(listOf(IntVariable(1), IntVariable(2)))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `nested MapVariable equals`() {
        val a = MapVariable(mapOf("k" to DoubleVariable(1.5)))
        val b = MapVariable(mapOf("k" to DoubleVariable(1.5)))
        assertEquals(a, b)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  Variable 接口多态
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `all types implement Variable interface`() {
        val vars: List<Variable<*>> = listOf(
            NullVariable(null),
            ByteVariable(1),
            ShortVariable(2),
            IntVariable(3),
            LongVariable(4L),
            FloatVariable(5f),
            DoubleVariable(6.0),
            CharVariable('c'),
            StringVariable("s"),
            BooleanVariable(true),
            ArrayVariable(emptyList()),
            MapVariable(emptyMap())
        )
        assertEquals(12, vars.size)
    }
}
