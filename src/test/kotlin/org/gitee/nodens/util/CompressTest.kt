package org.gitee.nodens.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CompressTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  文本压缩/解压往返
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `文本压缩解压往返 - 普通字符串`() {
        val original = "Hello, World! 你好世界"
        val compressed = compress(original)
        val decompressed = decompress(compressed)
        assertEquals(original, decompressed)
    }

    @Test
    fun `文本压缩解压往返 - 空字符串`() {
        val original = ""
        val compressed = compress(original)
        val decompressed = decompress(compressed)
        assertEquals(original, decompressed)
    }

    @Test
    fun `文本压缩解压往返 - 长文本`() {
        val original = "a".repeat(10000) + "中文".repeat(5000)
        val compressed = compress(original)
        val decompressed = decompress(compressed)
        assertEquals(original, decompressed)
    }

    @Test
    fun `文本压缩解压往返 - 特殊字符`() {
        val original = "换行\n制表\t回车\r空字符\u0000emoji🎮"
        val compressed = compress(original)
        val decompressed = decompress(compressed)
        assertEquals(original, decompressed)
    }

    @Test
    fun `文本压缩后体积应小于或等于原始数据`() {
        // 对于重复数据，压缩应该有效
        val original = "abcdefg".repeat(1000)
        val compressed = compress(original)
        assertTrue(compressed.size < original.toByteArray(Charsets.UTF_8).size,
            "压缩后 ${compressed.size} 应小于原始 ${original.toByteArray(Charsets.UTF_8).size}")
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  字节数组压缩/解压往返
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `字节数组压缩解压往返`() {
        val original = byteArrayOf(0, 1, 2, 3, 127, -128, -1)
        val compressed = compress(original)
        val decompressed = decompressToBytes(compressed)
        assertArrayEquals(original, decompressed)
    }

    @Test
    fun `空字节数组压缩解压往返`() {
        val original = byteArrayOf()
        val compressed = compress(original)
        val decompressed = decompressToBytes(compressed)
        assertArrayEquals(original, decompressed)
    }

    @Test
    fun `大字节数组压缩解压往返`() {
        val original = ByteArray(100000) { (it % 256).toByte() }
        val compressed = compress(original)
        val decompressed = decompressToBytes(compressed)
        assertArrayEquals(original, decompressed)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  错误处理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `解压无效数据应抛出 RuntimeException`() {
        assertThrows(RuntimeException::class.java) {
            decompress(byteArrayOf(0x7F, 0x00, 0x01, 0x02))
        }
    }

    @Test
    fun `解压字节无效数据应抛出 RuntimeException`() {
        assertThrows(RuntimeException::class.java) {
            decompressToBytes(byteArrayOf(0x7F, 0x00, 0x01, 0x02))
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  交叉验证
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `文本压缩结果可用 decompressToBytes 解压`() {
        val original = "Hello World"
        val compressed = compress(original)
        val decompressedBytes = decompressToBytes(compressed)
        assertEquals(original, String(decompressedBytes, Charsets.UTF_8))
    }

    @Test
    fun `字节压缩结果可用 decompress 解压为文本`() {
        val originalText = "测试文本"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)
        val compressed = compress(originalBytes)
        val decompressedText = decompress(compressed)
        assertEquals(originalText, decompressedText)
    }
}
