package org.gitee.nodens.util

import org.xerial.snappy.Snappy

/**
 * 压缩文本为字节数组
 */
fun compress(text: String): ByteArray {
    return try {
        Snappy.compress(text.toByteArray(Charsets.UTF_8))
    } catch (e: Exception) {
        throw RuntimeException("文本压缩失败", e)
    }
}

/**
 * 解压字节数组为文本
 */
fun decompress(compressedData: ByteArray): String {
    return try {
        val decompressedBytes = Snappy.uncompress(compressedData)
        String(decompressedBytes, Charsets.UTF_8)
    } catch (e: Exception) {
        throw RuntimeException("文本解压失败", e)
    }
}