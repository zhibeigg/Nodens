package org.gitee.nodens.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FastMatchingMapTest {

    private lateinit var map: FastMatchingMap<String>

    @BeforeEach
    fun setUp() {
        map = FastMatchingMap()
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  put / get 基础
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `put and get basic`() {
        map.put("攻击力", "ATK")
        assertEquals("ATK", map.get("攻击力"))
    }

    @Test
    fun `get returns null for unknown key`() {
        map.put("攻击力", "ATK")
        assertNull(map.get("防御力"))
    }

    @Test
    fun `put overwrites existing value`() {
        map.put("攻击力", "ATK1")
        map.put("攻击力", "ATK2")
        assertEquals("ATK2", map.get("攻击力"))
    }

    @Test
    fun `size tracks root chars`() {
        assertEquals(0, map.size)
        map.put("攻击力", "ATK")
        assertEquals(1, map.size)
        map.put("防御力", "DEF")
        assertEquals(2, map.size)
        // 同首字符不增加 size
        map.put("攻速", "SPD")
        assertEquals(2, map.size)
    }

    @Test
    fun `clear resets everything`() {
        map.put("攻击力", "ATK")
        map.put("防御力", "DEF")
        map.clear()
        assertEquals(0, map.size)
        assertNull(map.get("攻击力"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  getMatchResult
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getMatchResult returns value and remain`() {
        map.put("攻击力", "ATK")
        val result = map.getMatchResult("攻击力+100")
        assertNotNull(result)
        assertEquals("ATK", result!!.value)
        assertEquals("+100", result.remain)
    }

    @Test
    fun `getMatchResult remain is null when nothing left`() {
        map.put("攻击力", "ATK")
        val result = map.getMatchResult("攻击力")
        assertNotNull(result)
        assertEquals("ATK", result!!.value)
        assertNull(result.remain)
    }

    @Test
    fun `getMatchResult returns null for no match`() {
        map.put("攻击力", "ATK")
        assertNull(map.getMatchResult("防御力+50"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  shortest match
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `shortest match wins`() {
        map.put("攻击", "SHORT")
        map.put("攻击力", "LONG")
        assertEquals("SHORT", map.get("攻击力+100"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  ignoreSpace
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `ignoreSpace strips spaces from lore`() {
        map.put("攻击力", "ATK")
        assertEquals("ATK", map.get("攻 击 力"))
        assertEquals("ATK", map.get("  攻击力  "))
    }

    @Test
    fun `ignoreSpace disabled preserves spaces`() {
        val m = FastMatchingMap<String>(ignoreSpace = false)
        m.put("攻击力", "ATK")
        assertNull(m.get("攻 击 力"))
        assertEquals("ATK", m.get("攻击力"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  ignoreColor
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `ignoreColor strips color codes`() {
        map.put("攻击力", "ATK")
        assertEquals("ATK", map.get("§7攻击力"))
        assertEquals("ATK", map.get("&c攻击力"))
    }

    @Test
    fun `ignoreColor disabled preserves color codes`() {
        // ignorePrefix=true 时颜色代码保留但会被跳过找到首字符，所以同时禁用 prefix
        val m = FastMatchingMap<String>(ignoreColor = false, ignorePrefix = false)
        m.put("攻击力", "ATK")
        assertNull(m.get("§7攻击力"))
        assertEquals("ATK", m.get("攻击力"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  ignoreColon
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `ignoreColon strips colons`() {
        map.put("攻击力", "ATK")
        assertEquals("ATK", map.get("攻击力: +100"))
        assertEquals("ATK", map.get("攻击力：+100"))
    }

    @Test
    fun `ignoreColon disabled preserves colons`() {
        // 当 ignoreColon=false 时，冒号不会被去除
        // 如果 key 中包含冒号，则必须精确匹配
        val m = FastMatchingMap<String>(ignoreColon = false, ignorePrefix = false)
        m.put("攻击力:", "ATK")
        // 冒号保留，可以匹配
        assertEquals("ATK", m.get("攻击力:+100"))
        // 没有冒号则匹配不到（因为 key 是 "攻击力:"）
        assertNull(m.get("攻击力+100"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  ignorePrefix
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `ignorePrefix matches from any position`() {
        map.put("攻击力", "ATK")
        assertEquals("ATK", map.get("§7  攻击力: +100"))
    }

    @Test
    fun `ignorePrefix disabled requires match from start`() {
        val m = FastMatchingMap<String>(ignorePrefix = false)
        m.put("攻击力", "ATK")
        assertEquals("ATK", m.get("攻击力+100"))
        // 前面有其他字符时匹配失败（预处理后 "X攻击力" 第一个字符不匹配）
        assertNull(m.get("X攻击力+100"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  组合过滤
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `all filters combined`() {
        map.put("攻击力", "ATK")
        // 颜色 + 空格 + 冒号 + 前缀 全部忽略
        assertEquals("ATK", map.get("§7  攻击力：+100"))
    }

    @Test
    fun `all filters disabled`() {
        val m = FastMatchingMap<String>(
            ignoreSpace = false,
            ignoreColor = false,
            ignoreColon = false,
            ignorePrefix = false
        )
        m.put("攻击力", "ATK")
        assertEquals("ATK", m.get("攻击力+100"))
        assertNull(m.get("§7 攻击力: +100"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  边界情况
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `empty map returns null`() {
        assertNull(map.get("anything"))
        assertNull(map.getMatchResult("anything"))
    }

    @Test
    fun `empty string lore`() {
        map.put("攻击力", "ATK")
        assertNull(map.get(""))
    }

    @Test
    fun `multiple keys with different first chars`() {
        map.put("攻击力", "ATK")
        map.put("防御力", "DEF")
        map.put("暴击率", "CRIT")
        assertEquals("ATK", map.get("攻击力+10"))
        assertEquals("DEF", map.get("防御力+20"))
        assertEquals("CRIT", map.get("暴击率+5%"))
    }
}
