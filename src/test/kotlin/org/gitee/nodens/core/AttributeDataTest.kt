package org.gitee.nodens.core

import io.mockk.every
import io.mockk.mockk
import org.gitee.nodens.common.DigitalParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AttributeDataTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  数据持有
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `attributeNumber 正确持有`() {
        val mockNumber = mockk<IAttributeGroup.Number>(relaxed = true)
        val value = DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf(100.0))
        val data = AttributeData(mockNumber, value)
        assertSame(mockNumber, data.attributeNumber)
    }

    @Test
    fun `value 正确持有`() {
        val mockNumber = mockk<IAttributeGroup.Number>(relaxed = true)
        val value = DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf(100.0))
        val data = AttributeData(mockNumber, value)
        assertSame(value, data.value)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  toString
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `toString 包含组名和属性名`() {
        val mockGroup = mockk<IAttributeGroup> {
            every { name } returns "Damage"
        }
        val mockNumber = mockk<IAttributeGroup.Number> {
            every { group } returns mockGroup
            every { name } returns "Physical"
            every { config } returns mockk(relaxed = true)
        }
        val value = DigitalParser.Value(DigitalParser.Type.COUNT, doubleArrayOf(100.0))
        val data = AttributeData(mockNumber, value)
        val str = data.toString()
        assertTrue(str.contains("Damage"))
        assertTrue(str.contains("Physical"))
        assertTrue(str.contains("COUNT"))
    }

    @Test
    fun `toString 包含 PERCENT 类型`() {
        val mockGroup = mockk<IAttributeGroup> {
            every { name } returns "Crit"
        }
        val mockNumber = mockk<IAttributeGroup.Number> {
            every { group } returns mockGroup
            every { name } returns "Rate"
            every { config } returns mockk(relaxed = true)
        }
        val value = DigitalParser.Value(DigitalParser.Type.PERCENT, doubleArrayOf(0.15))
        val data = AttributeData(mockNumber, value)
        val str = data.toString()
        assertTrue(str.contains("PERCENT"))
    }
}
