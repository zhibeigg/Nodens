package org.gitee.nodens.module.item.drop

import io.mockk.mockk
import org.bukkit.entity.Item
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DropUserInfoTest {

    private fun createInfo(dropSurvival: Long): DropUser.Info {
        val mockItem = mockk<Item>(relaxed = true)
        return DropUser.Info(mockItem, dropSurvival)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  survival
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `survival 刚创建时接近 0`() {
        val info = createInfo(5000)
        assertTrue(info.survival < 50, "刚创建的 survival 应接近 0，实际: ${info.survival}")
    }

    @Test
    fun `survival 随时间增长`() {
        val info = createInfo(5000)
        Thread.sleep(50)
        assertTrue(info.survival >= 40, "等待 50ms 后 survival 应 >= 40，实际: ${info.survival}")
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  countdown
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `countdown 刚创建时接近 dropSurvival`() {
        val info = createInfo(5000)
        assertTrue(info.countdown in 4900..5000, "刚创建的 countdown 应接近 5000，实际: ${info.countdown}")
    }

    @Test
    fun `countdown 过期后为 0`() {
        val info = createInfo(10)
        Thread.sleep(20)
        assertEquals(0L, info.countdown)
    }

    @Test
    fun `countdown 不会为负数`() {
        val info = createInfo(1)
        Thread.sleep(10)
        assertTrue(info.countdown >= 0, "countdown 不应为负数")
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  dead
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `刚创建时 dead 为 false`() {
        val info = createInfo(5000)
        assertFalse(info.dead)
    }

    @Test
    fun `过期后 dead 为 true`() {
        val info = createInfo(10)
        Thread.sleep(20)
        assertTrue(info.dead)
    }
}
