package org.gitee.nodens.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UtilTest {

    // mergeValues / mergeMaps 依赖 DigitalParser（间接依赖 taboolib），无法在纯测试环境中测试

    @Test
    fun `comparePriority returns 1 when o1 greater`() {
        assertEquals(1, comparePriority(5, 3))
    }

    @Test
    fun `comparePriority returns -1 when o1 less`() {
        assertEquals(-1, comparePriority(3, 5))
    }

    @Test
    fun `comparePriority returns 0 when equal`() {
        assertEquals(0, comparePriority(7, 7))
    }

    @Test
    fun `comparePriority with negative numbers`() {
        assertEquals(-1, comparePriority(-5, 3))
        assertEquals(1, comparePriority(3, -5))
        assertEquals(0, comparePriority(-3, -3))
    }

    @Test
    fun `comparePriority with zero`() {
        assertEquals(1, comparePriority(1, 0))
        assertEquals(-1, comparePriority(0, 1))
        assertEquals(0, comparePriority(0, 0))
    }

    @Test
    fun `comparePriority with extreme values`() {
        assertEquals(1, comparePriority(Int.MAX_VALUE, Int.MIN_VALUE))
        assertEquals(-1, comparePriority(Int.MIN_VALUE, Int.MAX_VALUE))
    }
}
