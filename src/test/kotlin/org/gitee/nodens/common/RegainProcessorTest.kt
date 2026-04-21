package org.gitee.nodens.common

import io.mockk.*
import org.bukkit.entity.LivingEntity
import org.gitee.nodens.core.IAttributeGroup
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegainProcessorTest {

    private lateinit var mockHealer: LivingEntity
    private lateinit var mockPassive: LivingEntity
    private lateinit var mockNumber: IAttributeGroup.Number

    @BeforeEach
    fun setUp() {
        mockHealer = mockk(relaxed = true)
        mockPassive = mockk(relaxed = true)
        mockNumber = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun createProcessor(reason: String = "natural"): RegainProcessor {
        return RegainProcessor(reason, mockHealer, mockPassive)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  构造与基本属性
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `reason 自动转大写`() {
        val processor = createProcessor("natural")
        assertEquals("NATURAL", processor.reason)
    }

    @Test
    fun `scale 默认值为 1_0`() {
        val processor = createProcessor()
        assertEquals(1.0, processor.scale)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  RegainSource 管理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `addRegainSource 后可通过 getRegainSource 获取`() {
        val processor = createProcessor()
        processor.addRegainSource("heal", mockNumber, 50.0)
        val source = processor.getRegainSource("heal")
        assertNotNull(source)
        assertEquals("heal", source!!.key)
        assertEquals(50.0, source.regain)
        assertSame(mockNumber, source.attribute)
    }

    @Test
    fun `getRegainSource 不存在的 key 返回 null`() {
        val processor = createProcessor()
        assertNull(processor.getRegainSource("nonexistent"))
    }

    @Test
    fun `addRegainSource 覆盖同 key 的旧值`() {
        val processor = createProcessor()
        processor.addRegainSource("heal", mockNumber, 50.0)
        processor.addRegainSource("heal", mockNumber, 100.0)
        assertEquals(100.0, processor.getRegainSource("heal")!!.regain)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  ReduceSource 管理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `addReduceSource 后可通过 getReduceSource 获取`() {
        val processor = createProcessor()
        processor.addReduceSource("debuff", mockNumber, 20.0)
        val source = processor.getReduceSource("debuff")
        assertNotNull(source)
        assertEquals("debuff", source!!.key)
        assertEquals(20.0, source.reduce)
    }

    @Test
    fun `getReduceSource 不存在的 key 返回 null`() {
        val processor = createProcessor()
        assertNull(processor.getReduceSource("nonexistent"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  缓存与 refresh
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getFinalRegain 使用缓存`() {
        mockkObject(Handle)
        every { Handle.runProcessor(any<RegainProcessor>()) } returns 50.0

        val processor = createProcessor()
        val first = processor.getFinalRegain()
        val second = processor.getFinalRegain()

        assertEquals(50.0, first)
        assertEquals(50.0, second)
        verify(exactly = 1) { Handle.runProcessor(any<RegainProcessor>()) }
    }

    @Test
    fun `refresh 清除缓存后重新计算`() {
        mockkObject(Handle)
        every { Handle.runProcessor(any<RegainProcessor>()) } returnsMany listOf(50.0, 80.0)

        val processor = createProcessor()
        assertEquals(50.0, processor.getFinalRegain())
        processor.refresh()
        assertEquals(80.0, processor.getFinalRegain())

        verify(exactly = 2) { Handle.runProcessor(any<RegainProcessor>()) }
    }

    @Test
    fun `addRegainSource 自动触发 refresh`() {
        mockkObject(Handle)
        every { Handle.runProcessor(any<RegainProcessor>()) } returnsMany listOf(50.0, 80.0)

        val processor = createProcessor()
        assertEquals(50.0, processor.getFinalRegain())
        processor.addRegainSource("extra", mockNumber, 30.0)
        assertEquals(80.0, processor.getFinalRegain())
    }

    @Test
    fun `getFinalRegain 负值被 coerceAtLeast 0`() {
        mockkObject(Handle)
        every { Handle.runProcessor(any<RegainProcessor>()) } returns -30.0

        val processor = createProcessor()
        assertEquals(0.0, processor.getFinalRegain())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  callback 优先级排序
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `callback 按 priority 升序执行`() {
        val processor = createProcessor()
        val order = mutableListOf<Int>()

        processor.onRegain(3) { order.add(3) }
        processor.onRegain(1) { order.add(1) }
        processor.onRegain(2) { order.add(2) }

        processor.callback(50.0)

        assertEquals(listOf(1, 2, 3), order)
    }

    @Test
    fun `callback 传递正确的恢复值`() {
        val processor = createProcessor()
        var received = 0.0

        processor.onRegain(1) { regain -> received = regain }
        processor.callback(25.5)

        assertEquals(25.5, received)
    }

    @Test
    fun `无回调时 callback 不抛异常`() {
        val processor = createProcessor()
        assertDoesNotThrow { processor.callback(50.0) }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  RegainSource / ReduceSource toString
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `RegainSource toString 包含关键信息`() {
        val source = RegainProcessor.RegainSource("heal", mockNumber, 50.0)
        val str = source.toString()
        assertTrue(str.contains("heal"))
        assertTrue(str.contains("50.0"))
    }

    @Test
    fun `ReduceSource toString 包含关键信息`() {
        val source = RegainProcessor.ReduceSource("debuff", mockNumber, 20.0)
        val str = source.toString()
        assertTrue(str.contains("debuff"))
        assertTrue(str.contains("20.0"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  NATURAL_REASON 常量
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `NATURAL_REASON 常量值正确`() {
        assertEquals("NATURAL", RegainProcessor.NATURAL_REASON)
    }
}
