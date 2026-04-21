package org.gitee.nodens.core

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import taboolib.library.configuration.ConfigurationSection

class AttributeConfigTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  基本属性读取
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun mockSection(
        keys: List<String> = listOf("攻击力", "ATK"),
        valueType: String = "SINGLE",
        combatPower: Double = 1.0,
        syncPriority: Int = 0,
        handlePriority: Int = 0
    ): ConfigurationSection {
        return mockk {
            every { getStringList("keys") } returns keys
            every { getString("valueType", "SINGLE") } returns valueType
            every { getDouble("combatPower") } returns combatPower
            every { getInt("syncPriority") } returns syncPriority
            every { getInt("handlePriority") } returns handlePriority
        }
    }

    @Test
    fun `keys 从 getStringList 读取`() {
        val config = AttributeConfig(mockSection(keys = listOf("攻击力", "ATK", "Attack")))
        assertEquals(listOf("攻击力", "ATK", "Attack"), config.keys)
    }

    @Test
    fun `valueType 解析为 SINGLE`() {
        val config = AttributeConfig(mockSection(valueType = "SINGLE"))
        assertEquals(IAttributeGroup.Number.ValueType.SINGLE, config.valueType)
    }

    @Test
    fun `valueType 解析为 RANGE`() {
        val config = AttributeConfig(mockSection(valueType = "RANGE"))
        assertEquals(IAttributeGroup.Number.ValueType.RANGE, config.valueType)
    }

    @Test
    fun `valueType 大小写不敏感`() {
        val config = AttributeConfig(mockSection(valueType = "range"))
        assertEquals(IAttributeGroup.Number.ValueType.RANGE, config.valueType)
    }

    @Test
    fun `combatPower 正确读取`() {
        val config = AttributeConfig(mockSection(combatPower = 2.5))
        assertEquals(2.5, config.combatPower)
    }

    @Test
    fun `syncPriority 正确读取`() {
        val config = AttributeConfig(mockSection(syncPriority = 10))
        assertEquals(10, config.syncPriority)
    }

    @Test
    fun `handlePriority 正确读取`() {
        val config = AttributeConfig(mockSection(handlePriority = 5))
        assertEquals(5, config.handlePriority)
    }
}
