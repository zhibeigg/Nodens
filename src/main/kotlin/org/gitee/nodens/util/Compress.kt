package org.gitee.nodens.util

import org.xerial.snappy.Snappy

/**
 * 使用 Snappy 压缩文本为字节数组
 *
 * @param text 要压缩的文本
 * @return 压缩后的字节数组
 * @throws RuntimeException 如果压缩失败
 */
fun compress(text: String): ByteArray {
    return try {
        Snappy.compress(text.toByteArray(Charsets.UTF_8))
    } catch (e: Exception) {
        throw RuntimeException("文本压缩失败", e)
    }
}

/**
 * 使用 Snappy 压缩字节数组
 *
 * @param data 要压缩的字节数组
 * @return 压缩后的字节数组
 * @throws RuntimeException 如果压缩失败
 */
fun compress(data: ByteArray): ByteArray {
    return try {
        Snappy.compress(data)
    } catch (e: Exception) {
        throw RuntimeException("数据压缩失败", e)
    }
}

/**
 * 使用 Snappy 解压字节数组为文本
 *
 * @param compressedData 压缩的字节数组
 * @return 解压后的文本
 * @throws RuntimeException 如果解压失败
 */
fun decompress(compressedData: ByteArray): String {
    return try {
        val decompressedBytes = Snappy.uncompress(compressedData)
        String(decompressedBytes, Charsets.UTF_8)
    } catch (e: Exception) {
        throw RuntimeException("文本解压失败", e)
    }
}

/**
 * 使用 Snappy 解压字节数组
 *
 * @param compressedData 压缩的字节数组
 * @return 解压后的字节数组
 * @throws RuntimeException 如果解压失败
 */
fun decompressToBytes(compressedData: ByteArray): ByteArray {
    return try {
        Snappy.uncompress(compressedData)
    } catch (e: Exception) {
        throw RuntimeException("数据解压失败", e)
    }
}
