package org.gitee.nodens.core.reload

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReloadAPITest {

    @Test
    fun `registerReloadHook 返回成功并可注销`() {
        val owner = "test-owner-${System.nanoTime()}"
        val register = ReloadAPI.registerReloadHook(owner, 0, Runnable { })
        val unregister = ReloadAPI.unregisterReloadHooks(owner)

        assertTrue(register.success)
        assertTrue(unregister.success)
    }

    @Test
    fun `unregisterReloadHooks 对不存在 owner 返回失败`() {
        val result = ReloadAPI.unregisterReloadHooks("missing-owner-${System.nanoTime()}")

        assertFalse(result.success)
    }
}
