package org.gitee.nodens.module.item

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NormalContextTest {

    private fun ctx(vararg pairs: Pair<String, Variable<*>>): NormalContext {
        val map = hashMapOf(*pairs)
        return NormalContext("test", map, 0)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  get
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `get returns primitive value`() {
        val c = ctx("name" to StringVariable("sword"), "damage" to IntVariable(100))
        assertEquals("sword", c["name"])
        assertEquals(100, c["damage"])
    }

    @Test
    fun `get returns null for missing key`() {
        val c = ctx()
        assertNull(c["missing"])
    }

    @Test
    fun `get returns null for NullVariable`() {
        val c = ctx("n" to NullVariable(null))
        assertNull(c["n"])
    }

    @Test
    fun `get restores nested list`() {
        val c = ctx("tags" to ArrayVariable(listOf(StringVariable("a"), StringVariable("b"))))
        val tags = c["tags"]
        assertTrue(tags is List<*>)
        assertEquals(listOf("a", "b"), tags)
    }

    @Test
    fun `get restores nested map`() {
        val c = ctx("meta" to MapVariable(mapOf("k" to IntVariable(1))))
        val meta = c["meta"]
        assertTrue(meta is Map<*, *>)
        assertEquals(1, (meta as Map<*, *>)["k"])
    }

    @Test
    fun `get restores deeply nested structure`() {
        val inner = ArrayVariable(listOf(IntVariable(1), IntVariable(2)))
        val outer = MapVariable(mapOf("list" to inner, "name" to StringVariable("test")))
        val c = ctx("data" to outer)
        val data = c["data"] as Map<*, *>
        assertEquals(listOf(1, 2), data["list"])
        assertEquals("test", data["name"])
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  remove
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `remove returns variable and removes key`() {
        val c = ctx("x" to IntVariable(42))
        // remove 返回的是 Variable 对象（restore 不解包 Variable）
        val removed = c.remove("x")
        assertTrue(removed is IntVariable)
        assertEquals(42, (removed as IntVariable).value)
        assertNull(c["x"])
    }

    @Test
    fun `remove returns null for missing key`() {
        val c = ctx()
        assertNull(c.remove("missing"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  map / sourceMap
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `map returns all entries with restored values`() {
        val c = ctx("a" to IntVariable(1), "b" to StringVariable("two"))
        val m = c.map()
        assertEquals(2, m.size)
        assertEquals(1, m["a"])
        assertEquals("two", m["b"])
    }

    @Test
    fun `sourceMap returns raw variables`() {
        val c = ctx("x" to IntVariable(10))
        val sm = c.sourceMap()
        assertTrue(sm["x"] is IntVariable)
        assertEquals(10, (sm["x"] as IntVariable).value)
    }

    @Test
    fun `sourceMap is a copy`() {
        val c = ctx("x" to IntVariable(10))
        val sm1 = c.sourceMap()
        val sm2 = c.sourceMap()
        assertEquals(sm1, sm2)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  key / hashcode
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `key and hashcode are preserved`() {
        val c = NormalContext("weapon_01", hashMapOf(), 12345)
        assertEquals("weapon_01", c.key)
        assertEquals(12345, c.hashcode)
    }
}
