package org.gitee.nodens.util

import org.gitee.nodens.module.item.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ItemVariableTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  null 和 Variable 原样返回
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `null 转换为 NullVariable`() {
        val result = null.toVariable()
        assertInstanceOf(NullVariable::class.java, result)
        assertNull(result.value)
    }

    @Test
    fun `Variable 原样返回`() {
        val original = IntVariable(42)
        val result = (original as Any).toVariable()
        assertSame(original, result)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  基本类型转换
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `Byte 转换为 ByteVariable`() {
        val result = (42.toByte() as Any).toVariable()
        assertInstanceOf(ByteVariable::class.java, result)
        assertEquals(42.toByte(), result.value)
    }

    @Test
    fun `Short 转换为 ShortVariable`() {
        val result = (1000.toShort() as Any).toVariable()
        assertInstanceOf(ShortVariable::class.java, result)
        assertEquals(1000.toShort(), result.value)
    }

    @Test
    fun `Int 转换为 IntVariable`() {
        val result = (100 as Any).toVariable()
        assertInstanceOf(IntVariable::class.java, result)
        assertEquals(100, result.value)
    }

    @Test
    fun `Long 转换为 LongVariable`() {
        val result = (999999999L as Any).toVariable()
        assertInstanceOf(LongVariable::class.java, result)
        assertEquals(999999999L, result.value)
    }

    @Test
    fun `Float 转换为 FloatVariable`() {
        val result = (3.14f as Any).toVariable()
        assertInstanceOf(FloatVariable::class.java, result)
        assertEquals(3.14f, result.value)
    }

    @Test
    fun `Double 转换为 DoubleVariable`() {
        val result = (0.5 as Any).toVariable()
        assertInstanceOf(DoubleVariable::class.java, result)
        assertEquals(0.5, result.value)
    }

    @Test
    fun `Char 转换为 CharVariable`() {
        val result = ('A' as Any).toVariable()
        assertInstanceOf(CharVariable::class.java, result)
        assertEquals('A', result.value)
    }

    @Test
    fun `String 转换为 StringVariable`() {
        val result = ("hello" as Any).toVariable()
        assertInstanceOf(StringVariable::class.java, result)
        assertEquals("hello", result.value)
    }

    @Test
    fun `Boolean 转换为 BooleanVariable`() {
        val result = (true as Any).toVariable()
        assertInstanceOf(BooleanVariable::class.java, result)
        assertEquals(true, result.value)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  复合类型转换
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `List 转换为 ArrayVariable 递归`() {
        val list = listOf(1, "hello", true)
        val result = (list as Any).toVariable()
        assertInstanceOf(ArrayVariable::class.java, result)
        val arr = (result as ArrayVariable).value
        assertEquals(3, arr.size)
        assertInstanceOf(IntVariable::class.java, arr[0])
        assertInstanceOf(StringVariable::class.java, arr[1])
        assertInstanceOf(BooleanVariable::class.java, arr[2])
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `Map 转换为 MapVariable 递归`() {
        val map = mapOf("a" to 1, "b" to "hello")
        val result = (map as Any).toVariable()
        assertInstanceOf(MapVariable::class.java, result)
        val m = (result as MapVariable).value as Map<String, Variable<*>>
        assertEquals(2, m.size)
        assertInstanceOf(IntVariable::class.java, m["a"])
        assertInstanceOf(StringVariable::class.java, m["b"])
    }

    @Test
    fun `嵌套 List 中包含 Map`() {
        val nested = listOf(mapOf("x" to 1), mapOf("y" to 2))
        val result = (nested as Any).toVariable()
        assertInstanceOf(ArrayVariable::class.java, result)
        val arr = (result as ArrayVariable).value
        assertEquals(2, arr.size)
        arr.forEach { assertInstanceOf(MapVariable::class.java, it) }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  不支持的类型
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `不支持的类型抛出异常`() {
        // VariableRegistry.convert 在测试环境中未注册自定义转换器，返回 null
        // 因此 else 分支会抛出 IllegalStateException
        assertThrows(IllegalStateException::class.java) {
            (Object() as Any).toVariable()
        }
    }
}
