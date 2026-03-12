package org.gitee.nodens.module.item

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class VariableRegistryTest {

    @BeforeEach
    fun setUp() {
        VariableRegistry.rebuild()
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  register / isRegistered / getRegisteredNames
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    @Order(1)
    fun `builtin types are not in adapters`() {
        // 内置类型通过 subclass 注册，不在 adapters 中
        assertFalse(VariableRegistry.isRegistered(IntVariable::class.java))
        assertFalse(VariableRegistry.isRegistered(StringVariable::class.java))
    }

    @Test
    @Order(2)
    fun `getRegisteredNames is empty initially`() {
        assertTrue(VariableRegistry.getRegisteredNames().isEmpty())
    }

    @Test
    @Order(10)
    fun `register and check custom adapter`() {
        val adapter = TestVariableAdapter()
        VariableRegistry.register(adapter)
        VariableRegistry.rebuild()

        assertTrue(VariableRegistry.isRegistered(TestVariable::class.java))
        assertTrue(VariableRegistry.getRegisteredNames().contains("TestVariable"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  convert
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    @Order(3)
    fun `convert returns null for unknown type`() {
        assertNull(VariableRegistry.convert(Object()))
    }

    @Test
    @Order(11)
    fun `convert uses registered adapter`() {
        VariableRegistry.register(TestVariableAdapter())
        VariableRegistry.rebuild()

        val result = VariableRegistry.convert(TestData("hello"))
        assertNotNull(result)
        assertTrue(result is TestVariable)
        assertEquals("hello", (result as TestVariable).value)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  buildJson 序列化往返
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    @Order(4)
    fun `json roundtrip for builtin IntVariable`() {
        val json = VariableRegistry.json
        val original = IntVariable(42)
        val encoded = json.encodeToString(
            kotlinx.serialization.PolymorphicSerializer(Variable::class),
            original
        )
        val decoded = json.decodeFromString(
            kotlinx.serialization.PolymorphicSerializer(Variable::class),
            encoded
        )
        assertEquals(original, decoded)
    }

    @Test
    @Order(5)
    fun `json roundtrip for builtin StringVariable`() {
        val json = VariableRegistry.json
        val original = StringVariable("test")
        val encoded = json.encodeToString(
            kotlinx.serialization.PolymorphicSerializer(Variable::class),
            original
        )
        val decoded = json.decodeFromString(
            kotlinx.serialization.PolymorphicSerializer(Variable::class),
            encoded
        )
        assertEquals(original, decoded)
    }

    @Test
    @Order(6)
    fun `json roundtrip for ArrayVariable`() {
        val json = VariableRegistry.json
        val original = ArrayVariable(listOf(IntVariable(1), StringVariable("two")))
        val encoded = json.encodeToString(
            kotlinx.serialization.PolymorphicSerializer(Variable::class),
            original
        )
        val decoded = json.decodeFromString(
            kotlinx.serialization.PolymorphicSerializer(Variable::class),
            encoded
        )
        assertEquals(original, decoded)
    }

    @Test
    @Order(7)
    fun `rebuild produces working json instance`() {
        VariableRegistry.rebuild()
        val json = VariableRegistry.json
        assertNotNull(json)
        // 验证可以序列化
        val encoded = json.encodeToString(
            kotlinx.serialization.PolymorphicSerializer(Variable::class),
            BooleanVariable(true)
        )
        assertTrue(encoded.isNotEmpty())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  测试用辅助类
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    data class TestData(val text: String)

    data class TestVariable(override val value: String) : Variable<String>

    class TestVariableAdapter : VariableAdapter<TestVariable> {
        override val serialName = "TestVariable"
        override val variableClass = TestVariable::class.java

        override fun serialize(variable: TestVariable): String {
            return """{"value":"${variable.value}"}"""
        }

        override fun deserialize(json: String): TestVariable {
            val text = json.substringAfter("\"value\":\"").substringBefore("\"")
            return TestVariable(text)
        }

        override fun canConvert(value: Any): Boolean = value is TestData
        override fun convert(value: Any): TestVariable = TestVariable((value as TestData).text)
    }
}
