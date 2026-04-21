package org.gitee.nodens.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
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
