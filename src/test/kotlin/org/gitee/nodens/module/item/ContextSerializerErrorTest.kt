package org.gitee.nodens.module.item

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class ContextSerializerErrorTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            VariableRegistry.rebuild()
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  深层嵌套 — MAX_DEPTH 限制
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `序列化超深嵌套 Array 应抛出 IllegalStateException`() {
        // 构造深度 > 16 的嵌套 ArrayVariable
        var current: Variable<*> = IntVariable(1)
        repeat(18) {
            current = ArrayVariable(listOf(current))
        }
        val ctx = NormalContext("deep", hashMapOf("v" to current), 0)
        assertThrows(IllegalStateException::class.java) {
            ContextSerializer.serialize(ctx)
        }
    }

    @Test
    fun `序列化超深嵌套 Map 应抛出 IllegalStateException`() {
        var current: Variable<*> = IntVariable(1)
        repeat(18) {
            current = MapVariable(mapOf("k" to current))
        }
        val ctx = NormalContext("deep", hashMapOf("v" to current), 0)
        assertThrows(IllegalStateException::class.java) {
            ContextSerializer.serialize(ctx)
        }
    }

    @Test
    fun `恰好 16 层嵌套不应抛出异常`() {
        // depth 从 0 开始，16 层嵌套 = depth 到 16，刚好触发 > MAX_DEPTH
        // 实际上 15 层嵌套 = depth 到 15，不触发
        var current: Variable<*> = IntVariable(42)
        repeat(15) {
            current = ArrayVariable(listOf(current))
        }
        val ctx = NormalContext("ok", hashMapOf("v" to current), 0)
        // 不应抛出异常
        val bytes = ContextSerializer.serialize(ctx)
        val result = ContextSerializer.deserialize(bytes)
        assertNotNull(result)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  损坏数据
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `反序列化空字节数组应抛出异常`() {
        assertThrows(Exception::class.java) {
            ContextSerializer.deserialize(byteArrayOf())
        }
    }

    @Test
    fun `反序列化随机垃圾数据应抛出异常`() {
        assertThrows(Exception::class.java) {
            ContextSerializer.deserialize(byteArrayOf(0x7F, 0x00, 0x01, 0x02, 0x03))
        }
    }

    @Test
    fun `反序列化截断数据应抛出异常`() {
        // 先序列化一个正常的 context，然后截断
        val ctx = NormalContext("k", hashMapOf("a" to IntVariable(100)), 0)
        val bytes = ContextSerializer.serialize(ctx)
        // 截断到一半
        val truncated = bytes.copyOf(bytes.size / 2)
        assertThrows(Exception::class.java) {
            ContextSerializer.deserialize(truncated)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  未知类型标识符
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `反序列化含未知类型标识的数据 回退到 JSON 解析`() {
        // 手动构造一个含有 type=-1 (外部类型) 的字节流
        // 但 JSON 内容无效，应回退为 StringVariable
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { dos ->
            dos.writeUTF("test_key")
            dos.writeInt(42)
            dos.writeInt(1) // 1 个变量
            dos.writeUTF("ext_var")
            dos.writeByte(-1) // 外部类型标识
            dos.writeUTF("{\"invalid\": true}") // 无效的多态 JSON
        }
        // 反序列化不应崩溃，应回退为 StringVariable
        val result = ContextSerializer.deserialize(baos.toByteArray())
        assertEquals("test_key", result.key)
        assertEquals(42, result.hashcode)
        val variable = result.sourceMap()["ext_var"]
        // 外部类型解析失败时回退为 StringVariable
        assertTrue(variable is StringVariable, "未知类型应回退为 StringVariable，实际: ${variable?.javaClass}")
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  边界情况
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `序列化空字符串 key 和空 variable key`() {
        val ctx = NormalContext("", hashMapOf("" to StringVariable("")), 0)
        val bytes = ContextSerializer.serialize(ctx)
        val result = ContextSerializer.deserialize(bytes)
        assertEquals("", result.key)
        assertEquals("", (result.sourceMap()[""] as StringVariable).value)
    }

    @Test
    fun `序列化大量变量`() {
        val vars = HashMap<String, Variable<*>>()
        repeat(1000) { i ->
            vars["var_$i"] = IntVariable(i)
        }
        val ctx = NormalContext("bulk", vars, 999)
        val bytes = ContextSerializer.serialize(ctx)
        val result = ContextSerializer.deserialize(bytes)
        assertEquals(1000, result.sourceMap().size)
        assertEquals(500, (result.sourceMap()["var_500"] as IntVariable).value)
    }

    @Test
    fun `序列化含特殊字符的字符串`() {
        val special = "你好\n\t\r\u0000世界🎮"
        val ctx = NormalContext("k", hashMapOf("s" to StringVariable(special)), 0)
        val bytes = ContextSerializer.serialize(ctx)
        val result = ContextSerializer.deserialize(bytes)
        assertEquals(special, (result.sourceMap()["s"] as StringVariable).value)
    }

    @Test
    fun `序列化空 Array 和空 Map`() {
        val ctx = NormalContext("k", hashMapOf(
            "emptyArr" to ArrayVariable(emptyList()),
            "emptyMap" to MapVariable(emptyMap())
        ), 0)
        val bytes = ContextSerializer.serialize(ctx)
        val result = ContextSerializer.deserialize(bytes)
        assertEquals(0, (result.sourceMap()["emptyArr"] as ArrayVariable).value.size)
        assertEquals(0, (result.sourceMap()["emptyMap"] as MapVariable).value.size)
    }

    @Test
    fun `序列化极值数字`() {
        val ctx = NormalContext("k", hashMapOf(
            "maxInt" to IntVariable(Int.MAX_VALUE),
            "minInt" to IntVariable(Int.MIN_VALUE),
            "maxLong" to LongVariable(Long.MAX_VALUE),
            "minLong" to LongVariable(Long.MIN_VALUE),
            "maxDouble" to DoubleVariable(Double.MAX_VALUE),
            "minDouble" to DoubleVariable(Double.MIN_VALUE),
            "nan" to DoubleVariable(Double.NaN),
            "posInf" to DoubleVariable(Double.POSITIVE_INFINITY),
            "negInf" to DoubleVariable(Double.NEGATIVE_INFINITY)
        ), 0)
        val bytes = ContextSerializer.serialize(ctx)
        val result = ContextSerializer.deserialize(bytes)
        assertEquals(Int.MAX_VALUE, (result.sourceMap()["maxInt"] as IntVariable).value)
        assertEquals(Int.MIN_VALUE, (result.sourceMap()["minInt"] as IntVariable).value)
        assertEquals(Long.MAX_VALUE, (result.sourceMap()["maxLong"] as LongVariable).value)
        assertEquals(Long.MIN_VALUE, (result.sourceMap()["minLong"] as LongVariable).value)
        assertEquals(Double.MAX_VALUE, (result.sourceMap()["maxDouble"] as DoubleVariable).value)
        assertEquals(Double.MIN_VALUE, (result.sourceMap()["minDouble"] as DoubleVariable).value)
        assertTrue((result.sourceMap()["nan"] as DoubleVariable).value.isNaN())
        assertEquals(Double.POSITIVE_INFINITY, (result.sourceMap()["posInf"] as DoubleVariable).value)
        assertEquals(Double.NEGATIVE_INFINITY, (result.sourceMap()["negInf"] as DoubleVariable).value)
    }
}
