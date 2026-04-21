package org.gitee.nodens.module.item

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * NormalContext 写入侧测试
 *
 * 注意: set/putAll 依赖 toVariable() 扩展函数（位于 util/Item.kt），
 * 该文件依赖 Bukkit API，无法在纯测试环境中加载。
 * 因此这里通过直接操作底层 HashMap 来测试写入后的读取一致性。
 */
class NormalContextWriteTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  直接构造后读取一致性
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `构造后 get 返回正确的基本类型值`() {
        val vars = hashMapOf<String, Variable<*>>(
            "name" to StringVariable("sword"),
            "damage" to IntVariable(100),
            "rate" to DoubleVariable(0.5),
            "active" to BooleanVariable(true),
            "byte" to ByteVariable(42),
            "short" to ShortVariable(1000),
            "long" to LongVariable(999999999L),
            "float" to FloatVariable(3.14f),
            "char" to CharVariable('A')
        )
        val ctx = NormalContext("test", vars, 0)
        assertEquals("sword", ctx["name"])
        assertEquals(100, ctx["damage"])
        assertEquals(0.5, ctx["rate"])
        assertEquals(true, ctx["active"])
        assertEquals(42.toByte(), ctx["byte"])
        assertEquals(1000.toShort(), ctx["short"])
        assertEquals(999999999L, ctx["long"])
        assertEquals(3.14f, ctx["float"])
        assertEquals('A', ctx["char"])
    }

    @Test
    fun `构造后 map 返回所有值`() {
        val vars = hashMapOf<String, Variable<*>>(
            "a" to IntVariable(1),
            "b" to StringVariable("hello"),
            "c" to BooleanVariable(true)
        )
        val ctx = NormalContext("test", vars, 0)
        val map = ctx.map()
        assertEquals(3, map.size)
        assertEquals(1, map["a"])
        assertEquals("hello", map["b"])
        assertEquals(true, map["c"])
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  底层 HashMap 可变性验证
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `通过底层 HashMap 添加变量后 get 可读取`() {
        val vars = hashMapOf<String, Variable<*>>()
        val ctx = NormalContext("test", vars, 0)
        // 直接操作底层 map（模拟 set 的效果）
        vars["name"] = StringVariable("sword")
        vars["damage"] = IntVariable(100)
        assertEquals("sword", ctx["name"])
        assertEquals(100, ctx["damage"])
    }

    @Test
    fun `通过底层 HashMap 覆盖变量后 get 返回新值`() {
        val vars = hashMapOf<String, Variable<*>>("x" to IntVariable(1))
        val ctx = NormalContext("test", vars, 0)
        assertEquals(1, ctx["x"])
        vars["x"] = IntVariable(2)
        assertEquals(2, ctx["x"])
    }

    @Test
    fun `通过底层 HashMap 删除后 get 返回 null`() {
        val vars = hashMapOf<String, Variable<*>>("x" to IntVariable(1))
        val ctx = NormalContext("test", vars, 0)
        vars.remove("x")
        assertNull(ctx["x"])
    }

    @Test
    fun `通过底层 HashMap 批量添加`() {
        val vars = hashMapOf<String, Variable<*>>()
        val ctx = NormalContext("test", vars, 0)
        vars.putAll(mapOf(
            "a" to IntVariable(1),
            "b" to StringVariable("hello"),
            "c" to BooleanVariable(true)
        ))
        assertEquals(1, ctx["a"])
        assertEquals("hello", ctx["b"])
        assertEquals(true, ctx["c"])
        assertEquals(3, ctx.sourceMap().size)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  NullVariable 写入
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `NullVariable 写入后 get 返回 null 但 key 存在`() {
        val vars = hashMapOf<String, Variable<*>>("n" to NullVariable(null))
        val ctx = NormalContext("test", vars, 0)
        assertNull(ctx["n"])
        assertTrue(ctx.sourceMap().containsKey("n"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  复合类型写入
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `ArrayVariable 写入后 get 返回 List`() {
        val vars = hashMapOf<String, Variable<*>>(
            "tags" to ArrayVariable(listOf(StringVariable("fire"), StringVariable("rare")))
        )
        val ctx = NormalContext("test", vars, 0)
        assertEquals(listOf("fire", "rare"), ctx["tags"])
    }

    @Test
    fun `MapVariable 写入后 get 返回 Map`() {
        val vars = hashMapOf<String, Variable<*>>(
            "meta" to MapVariable(mapOf("level" to IntVariable(5), "name" to StringVariable("test")))
        )
        val ctx = NormalContext("test", vars, 0)
        val result = ctx["meta"] as Map<*, *>
        assertEquals(5, result["level"])
        assertEquals("test", result["name"])
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  写入 + 序列化往返
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `写入后序列化往返保持一致`() {
        VariableRegistry.rebuild()
        val vars = hashMapOf<String, Variable<*>>()
        val ctx = NormalContext("weapon", vars, 42)
        vars["name"] = StringVariable("sword")
        vars["damage"] = IntVariable(100)
        vars["tags"] = ArrayVariable(listOf(StringVariable("fire"), StringVariable("rare")))

        val bytes = ContextSerializer.serialize(ctx)
        val result = ContextSerializer.deserialize(bytes)

        assertEquals("weapon", result.key)
        assertEquals(42, result.hashcode)
        assertEquals("sword", result["name"])
        assertEquals(100, result["damage"])
        assertEquals(listOf("fire", "rare"), result["tags"])
    }

    @Test
    fun `remove 后序列化不包含已删除的 key`() {
        VariableRegistry.rebuild()
        val vars = hashMapOf<String, Variable<*>>(
            "a" to IntVariable(1),
            "b" to IntVariable(2)
        )
        val ctx = NormalContext("test", vars, 0)
        ctx.remove("a")

        val bytes = ContextSerializer.serialize(ctx)
        val result = ContextSerializer.deserialize(bytes)

        assertNull(result["a"])
        assertEquals(2, result["b"])
        assertEquals(1, result.sourceMap().size)
    }
}
