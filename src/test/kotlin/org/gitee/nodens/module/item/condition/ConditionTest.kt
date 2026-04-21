package org.gitee.nodens.module.item.condition

import io.mockk.*
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.module.item.condition.impl.BindCondition
import org.gitee.nodens.module.item.condition.impl.LevelCondition
import org.gitee.nodens.module.item.condition.impl.SlotCondition
import org.gitee.nodens.module.item.condition.impl.TimeCondition
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * 条件系统测试
 *
 * 注意: 条件类的 check 方法依赖 Bukkit 的 LivingEntity/ItemStack，
 * 而 Bukkit 接口在纯测试环境中无法可靠 mock（缺少 Guava 等传递依赖）。
 * 因此这里只测试不需要 Bukkit 实例的逻辑。
 */
class ConditionTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun initNodens() {
            mockkObject(Nodens)
            every { Nodens.config } returns mockk(relaxed = true)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  ConditionManager 常量
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `SLOT_DATA_KEY 常量值正确`() {
        assertEquals("slot", ConditionManager.SLOT_DATA_KEY)
    }

    @Test
    fun `SLOT_IDENTIFY_KEY 常量值正确`() {
        assertEquals("identify", ConditionManager.SLOT_IDENTIFY_KEY)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  ConditionManager 内部 map 操作
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `CONDITION_MATCHING_MAP 初始为空`() {
        ConditionManager.CONDITION_MATCHING_MAP.clear()
        assertEquals(0, ConditionManager.CONDITION_MATCHING_MAP.size)
    }

    @Test
    fun `conditionMap 可注册和查询条件`() {
        val condition = mockk<ICondition>(relaxed = true)
        ConditionManager.conditionMap["TestCondition"] = condition
        try {
            assertSame(condition, ConditionManager.conditionMap["TestCondition"])
        } finally {
            ConditionManager.conditionMap.remove("TestCondition")
        }
    }

    @Test
    fun `CONDITION_MATCHING_MAP 可注册和匹配条件`() {
        val condition = mockk<ICondition>(relaxed = true)
        ConditionManager.CONDITION_MATCHING_MAP.put("绑定", condition)
        try {
            val result = ConditionManager.CONDITION_MATCHING_MAP.getMatchResult("绑定TestPlayer")
            assertNotNull(result)
            assertSame(condition, result!!.value)
            assertEquals("TestPlayer", result.remain)
        } finally {
            ConditionManager.CONDITION_MATCHING_MAP.clear()
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  条件类实例验证
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `BindCondition 是 ICondition 实例`() {
        assertInstanceOf(ICondition::class.java, BindCondition)
    }

    @Test
    fun `LevelCondition 是 ICondition 实例`() {
        assertInstanceOf(ICondition::class.java, LevelCondition)
    }

    @Test
    fun `TimeCondition 是 ICondition 实例`() {
        assertInstanceOf(ICondition::class.java, TimeCondition)
    }

    @Test
    fun `SlotCondition 是 ICondition 实例`() {
        assertInstanceOf(ICondition::class.java, SlotCondition)
    }
}
