package org.gitee.nodens.common

import io.mockk.*
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.core.IAttributeGroup
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DamageProcessorTest {

    private lateinit var mockAttacker: LivingEntity
    private lateinit var mockDefender: LivingEntity
    private lateinit var mockNumber: IAttributeGroup.Number

    @BeforeEach
    fun setUp() {
        mockAttacker = mockk(relaxed = true)
        mockDefender = mockk(relaxed = true)
        mockNumber = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun createProcessor(damageType: String = "physical"): DamageProcessor {
        return DamageProcessor(damageType, mockAttacker, mockDefender)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  构造与基本属性
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `damageType 自动转大写`() {
        val processor = createProcessor("physical")
        assertEquals("PHYSICAL", processor.damageType)
    }

    @Test
    fun `scale 默认值为 1_0`() {
        val processor = createProcessor()
        assertEquals(1.0, processor.scale)
    }

    @Test
    fun `crit 默认值为 false`() {
        val processor = createProcessor()
        assertFalse(processor.crit)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  DamageSource 管理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `addDamageSource 后可通过 getDamageSource 获取`() {
        val processor = createProcessor()
        processor.addDamageSource("base", mockNumber, 100.0)
        val source = processor.getDamageSource("base")
        assertNotNull(source)
        assertEquals("base", source!!.key)
        assertEquals(100.0, source.damage)
        assertSame(mockNumber, source.attribute)
    }

    @Test
    fun `getDamageSource 不存在的 key 返回 null`() {
        val processor = createProcessor()
        assertNull(processor.getDamageSource("nonexistent"))
    }

    @Test
    fun `addDamageSource 覆盖同 key 的旧值`() {
        val processor = createProcessor()
        processor.addDamageSource("base", mockNumber, 100.0)
        processor.addDamageSource("base", mockNumber, 200.0)
        assertEquals(200.0, processor.getDamageSource("base")!!.damage)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  DefenceSource 管理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `addDefenceSource 后可通过 getDefenceSource 获取`() {
        val processor = createProcessor()
        processor.addDefenceSource("armor", mockNumber, 50.0)
        val source = processor.getDefenceSource("armor")
        assertNotNull(source)
        assertEquals("armor", source!!.key)
        assertEquals(50.0, source.defence)
    }

    @Test
    fun `getDefenceSource 不存在的 key 返回 null`() {
        val processor = createProcessor()
        assertNull(processor.getDefenceSource("nonexistent"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  缓存与 refresh
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getFinalDamage 使用缓存`() {
        mockkObject(Handle)
        every { Handle.runProcessor(any<DamageProcessor>()) } returns 100.0

        val processor = createProcessor()
        val first = processor.getFinalDamage()
        val second = processor.getFinalDamage()

        assertEquals(100.0, first)
        assertEquals(100.0, second)
        // Handle.runProcessor 只应被调用一次（缓存命中）
        verify(exactly = 1) { Handle.runProcessor(any<DamageProcessor>()) }
    }

    @Test
    fun `refresh 清除缓存后重新计算`() {
        mockkObject(Handle)
        every { Handle.runProcessor(any<DamageProcessor>()) } returnsMany listOf(100.0, 200.0)

        val processor = createProcessor()
        assertEquals(100.0, processor.getFinalDamage())
        processor.refresh()
        assertEquals(200.0, processor.getFinalDamage())

        verify(exactly = 2) { Handle.runProcessor(any<DamageProcessor>()) }
    }

    @Test
    fun `addDamageSource 自动触发 refresh`() {
        mockkObject(Handle)
        every { Handle.runProcessor(any<DamageProcessor>()) } returnsMany listOf(100.0, 150.0)

        val processor = createProcessor()
        assertEquals(100.0, processor.getFinalDamage())
        processor.addDamageSource("extra", mockNumber, 50.0)
        assertEquals(150.0, processor.getFinalDamage())
    }

    @Test
    fun `getFinalDamage 负值被 coerceAtLeast 0`() {
        mockkObject(Handle)
        every { Handle.runProcessor(any<DamageProcessor>()) } returns -50.0

        val processor = createProcessor()
        assertEquals(0.0, processor.getFinalDamage())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  callback 优先级排序
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `callback 按 priority 升序执行`() {
        val processor = createProcessor()
        val order = mutableListOf<Int>()

        processor.onDamage(3) { order.add(3) }
        processor.onDamage(1) { order.add(1) }
        processor.onDamage(2) { order.add(2) }

        processor.callback(100.0)

        assertEquals(listOf(1, 2, 3), order)
    }

    @Test
    fun `callback 传递正确的伤害值`() {
        val processor = createProcessor()
        var received = 0.0

        processor.onDamage(1) { damage -> received = damage }
        processor.callback(42.5)

        assertEquals(42.5, received)
    }

    @Test
    fun `无回调时 callback 不抛异常`() {
        val processor = createProcessor()
        assertDoesNotThrow { processor.callback(100.0) }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  DamageSource / DefenceSource toString
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `DamageSource toString 包含关键信息`() {
        val source = DamageProcessor.DamageSource("base", mockNumber, 100.0)
        val str = source.toString()
        assertTrue(str.contains("base"))
        assertTrue(str.contains("100.0"))
    }

    @Test
    fun `DefenceSource toString 包含关键信息`() {
        val source = DamageProcessor.DefenceSource("armor", mockNumber, 50.0)
        val str = source.toString()
        assertTrue(str.contains("armor"))
        assertTrue(str.contains("50.0"))
    }
}
