package org.gitee.nodens.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.gitee.nodens.api.AttributeRegistrationConfig
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.common.DigitalParser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.ConfigFile

class AttributeManagerTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun initNodens() {
            // AttributeManager 类加载时 healthScaled 会访问 Nodens.config
            // 需要先通过反射初始化 Nodens.config
            try {
                val field = Nodens::class.java.getDeclaredField("config")
                field.isAccessible = true
                field.set(Nodens, mockk<ConfigFile>(relaxed = true))
            } catch (_: Exception) {
                // 如果已经初始化则忽略
            }
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            unmockkAll()
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  ATTRIBUTE_COMPARATOR
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun mockNumber(handlePriority: Int, name: String): IAttributeGroup.Number {
        val config = mockk<AttributeConfig> {
            every { this@mockk.handlePriority } returns handlePriority
        }
        return mockk {
            every { this@mockk.config } returns config
            every { this@mockk.name } returns name
        }
    }

    private fun mockGroup(name: String): IAttributeGroup {
        return mockk {
            every { this@mockk.name } returns name
            every { this@mockk.numbers } returns emptyMap()
        }
    }

    @Test
    fun `ATTRIBUTE_COMPARATOR 按 handlePriority 排序`() {
        val low = mockNumber(1, "A")
        val high = mockNumber(10, "B")
        assertTrue(AttributeManager.ATTRIBUTE_COMPARATOR.compare(low, high) < 0)
        assertTrue(AttributeManager.ATTRIBUTE_COMPARATOR.compare(high, low) > 0)
    }

    @Test
    fun `ATTRIBUTE_COMPARATOR 相同优先级按 name 排序`() {
        val a = mockNumber(5, "Alpha")
        val b = mockNumber(5, "Beta")
        assertTrue(AttributeManager.ATTRIBUTE_COMPARATOR.compare(a, b) < 0)
        assertTrue(AttributeManager.ATTRIBUTE_COMPARATOR.compare(b, a) > 0)
    }

    @Test
    fun `ATTRIBUTE_COMPARATOR 完全相同返回 0`() {
        val a = mockNumber(5, "Same")
        val b = mockNumber(5, "Same")
        assertEquals(0, AttributeManager.ATTRIBUTE_COMPARATOR.compare(a, b))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  运行期属性组注册
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `registerAttributeGroup 延迟重载时写入注册表和当前映射`() {
        val group = mockGroup("RuntimeGroup")
        try {
            val previous = AttributeManager.registerAttributeGroup(group, reloadAttributes = false)

            assertNull(previous)
            assertSame(group, AttributeManager.getGroup("RuntimeGroup"))
            assertSame(group, AttributeManager.getRegisteredAttributeGroups()["RuntimeGroup"])
        } finally {
            AttributeManager.unregisterAttributeGroup("RuntimeGroup", reloadAttributes = false)
        }
    }

    @Test
    fun `registerAttributeGroup 同名注册返回旧实例并替换当前映射`() {
        val oldGroup = mockGroup("ReplaceGroup")
        val newGroup = mockGroup("ReplaceGroup")
        try {
            AttributeManager.registerAttributeGroup(oldGroup, reloadAttributes = false)
            val previous = AttributeManager.registerAttributeGroup(newGroup, reloadAttributes = false)

            assertSame(oldGroup, previous)
            assertSame(newGroup, AttributeManager.getGroup("ReplaceGroup"))
            assertSame(newGroup, AttributeManager.getRegisteredAttributeGroups()["ReplaceGroup"])
        } finally {
            AttributeManager.unregisterAttributeGroup("ReplaceGroup", reloadAttributes = false)
        }
    }

    @Test
    fun `unregisterAttributeGroup 移除运行期属性组`() {
        val group = mockGroup("RemoveGroup")
        AttributeManager.registerAttributeGroup(group, reloadAttributes = false)

        val removed = AttributeManager.unregisterAttributeGroup("RemoveGroup", reloadAttributes = false)

        assertSame(group, removed)
        assertNull(AttributeManager.getGroup("RemoveGroup"))
        assertFalse(AttributeManager.getRegisteredAttributeGroups().containsKey("RemoveGroup"))
    }

    @Test
    fun `registerAttributeGroup 支持纯内存配置`() {
        val config = AttributeRegistrationConfig(keys = listOf("运行期属性"), combatPower = 7.0, handlePriority = 2)
        val number = mockk<IAttributeGroup.Number>(relaxed = true)
        val group = mockk<IAttributeGroup> {
            every { name } returns "MemoryConfigGroup"
            every { numbers } returns mapOf("Runtime" to number)
        }
        every { number.group } returns group
        every { number.name } returns "Runtime"
        every { number.config } answers { AttributeManager.getConfig("MemoryConfigGroup", "Runtime") }

        try {
            val result = AttributeManager.registerAttributeGroup(
                group,
                mapOf("Runtime" to config),
                reloadAttributes = false,
            )

            assertTrue(result.success)
            assertSame(group, AttributeManager.getGroup("MemoryConfigGroup"))
            assertEquals(listOf("运行期属性"), AttributeManager.getConfig("MemoryConfigGroup", "Runtime").keys)
            assertSame(number, AttributeManager.ATTRIBUTE_MATCHING_MAP.get("运行期属性 10"))
        } finally {
            AttributeManager.unregisterAttributeGroup("MemoryConfigGroup", reloadAttributes = false)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  matchAttribute 通过预填充 ATTRIBUTE_MATCHING_MAP
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @BeforeEach
    fun setUp() {
        AttributeManager.ATTRIBUTE_MATCHING_MAP.clear()
    }

    @Test
    fun `matchAttribute 无匹配返回 null`() {
        assertNull(AttributeManager.matchAttribute("不存在的属性+100"))
    }

    @Test
    fun `matchAttribute 匹配但 remain 为 null 返回 null`() {
        val mockNumber = mockk<IAttributeGroup.Number>(relaxed = true)
        AttributeManager.ATTRIBUTE_MATCHING_MAP.put("攻击力", mockNumber)
        assertNull(AttributeManager.matchAttribute("攻击力"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  getCombatPower
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getCombatPower 空数组返回 0`() {
        assertEquals(0.0, AttributeManager.getCombatPower())
    }

    @Test
    fun `getCombatPower 正确累加`() {
        val mockNumber = mockk<IAttributeGroup.Number>(relaxed = true) {
            every { combatPower(any()) } returns 10.0
        }
        val value = DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf(100.0))
        val data = AttributeData(mockNumber, value)
        val result = AttributeManager.getCombatPower(data)
        assertEquals(10.0, result)
    }

    @Test
    fun `getCombatPower 多个同属性累加后计算`() {
        val mockNumber = mockk<IAttributeGroup.Number>(relaxed = true) {
            every { combatPower(any()) } returns 25.0
        }
        val v1 = DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf(50.0))
        val v2 = DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf(50.0))
        val d1 = AttributeData(mockNumber, v1)
        val d2 = AttributeData(mockNumber, v2)
        val result = AttributeManager.getCombatPower(d1, d2)
        assertEquals(25.0, result)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  getGroup / getNumber
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getGroup 不存在返回 null`() {
        assertNull(AttributeManager.getGroup("不存在的组"))
    }

    @Test
    fun `getGroup 存在时返回正确实例`() {
        val mockGroup = mockk<IAttributeGroup> {
            every { name } returns "TestGroup"
            every { numbers } returns emptyMap()
        }
        AttributeManager.groupMap["TestGroup"] = mockGroup
        try {
            assertSame(mockGroup, AttributeManager.getGroup("TestGroup"))
        } finally {
            AttributeManager.groupMap.remove("TestGroup")
        }
    }

    @Test
    fun `getNumber 不存在的组返回 null`() {
        assertNull(AttributeManager.getNumber("不存在", "不存在"))
    }

    @Test
    fun `getNumber 存在时返回正确实例`() {
        val mockNumber = mockk<IAttributeGroup.Number>(relaxed = true)
        val mockGroup = mockk<IAttributeGroup> {
            every { name } returns "TestGroup"
            every { numbers } returns mapOf("Physical" to mockNumber)
        }
        AttributeManager.groupMap["TestGroup"] = mockGroup
        try {
            assertSame(mockNumber, AttributeManager.getNumber("TestGroup", "Physical"))
        } finally {
            AttributeManager.groupMap.remove("TestGroup")
        }
    }
}
