package org.gitee.nodens.module.item.condition

import io.mockk.*
import org.gitee.nodens.api.Nodens
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * ConditionManager 测试
 *
 * 注意: matchConditions 方法依赖 Bukkit 的 LivingEntity/ItemStack/ItemMeta，
 * 这些接口在纯测试环境中无法可靠 mock（缺少 Guava 等传递依赖）。
 * 因此这里只测试 ConditionManager 的内部 map 管理逻辑。
 */
class ConditionManagerTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun initNodens() {
            mockkObject(Nodens)
            every { Nodens.config } returns mockk(relaxed = true)
        }
    }

    @BeforeEach
    fun setUp() {
        ConditionManager.CONDITION_MATCHING_MAP.clear()
        ConditionManager.conditionMap.clear()
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  conditionMap 管理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `conditionMap 初始为空`() {
        assertTrue(ConditionManager.conditionMap.isEmpty())
    }

    @Test
    fun `conditionMap 注册后可查询`() {
        val condition = mockk<ICondition>(relaxed = true)
        ConditionManager.conditionMap["Bind"] = condition
        assertEquals(1, ConditionManager.conditionMap.size)
        assertSame(condition, ConditionManager.conditionMap["Bind"])
    }

    @Test
    fun `conditionMap 注册多个条件`() {
        val c1 = mockk<ICondition>(relaxed = true)
        val c2 = mockk<ICondition>(relaxed = true)
        ConditionManager.conditionMap["Bind"] = c1
        ConditionManager.conditionMap["Level"] = c2
        assertEquals(2, ConditionManager.conditionMap.size)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  CONDITION_MATCHING_MAP 管理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `CONDITION_MATCHING_MAP 注册后可匹配`() {
        val condition = mockk<ICondition>(relaxed = true)
        ConditionManager.CONDITION_MATCHING_MAP.put("绑定", condition)
        val result = ConditionManager.CONDITION_MATCHING_MAP.getMatchResult("绑定TestPlayer")
        assertNotNull(result)
        assertSame(condition, result!!.value)
        assertEquals("TestPlayer", result.remain)
    }

    @Test
    fun `CONDITION_MATCHING_MAP 无匹配返回 null`() {
        assertNull(ConditionManager.CONDITION_MATCHING_MAP.getMatchResult("不存在的条件"))
    }

    @Test
    fun `CONDITION_MATCHING_MAP clear 后无匹配`() {
        val condition = mockk<ICondition>(relaxed = true)
        ConditionManager.CONDITION_MATCHING_MAP.put("绑定", condition)
        ConditionManager.CONDITION_MATCHING_MAP.clear()
        assertNull(ConditionManager.CONDITION_MATCHING_MAP.getMatchResult("绑定TestPlayer"))
    }
}
