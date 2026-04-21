package org.gitee.nodens.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FilesTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  getFiles 递归收集
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `收集 yml 文件`(@TempDir dir: File) {
        File(dir, "config.yml").createNewFile()
        File(dir, "data.yml").createNewFile()
        val result = getFiles(dir)
        assertEquals(2, result.size)
        assertTrue(result.all { it.extension == "yml" })
    }

    @Test
    fun `收集 js 文件`(@TempDir dir: File) {
        File(dir, "script.js").createNewFile()
        val result = getFiles(dir)
        assertEquals(1, result.size)
        assertEquals("js", result[0].extension)
    }

    @Test
    fun `忽略其他扩展名`(@TempDir dir: File) {
        File(dir, "readme.md").createNewFile()
        File(dir, "data.json").createNewFile()
        File(dir, "image.png").createNewFile()
        val result = getFiles(dir)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `递归遍历子目录`(@TempDir dir: File) {
        val sub = File(dir, "sub").apply { mkdir() }
        val deep = File(sub, "deep").apply { mkdir() }
        File(dir, "root.yml").createNewFile()
        File(sub, "sub.yml").createNewFile()
        File(deep, "deep.js").createNewFile()
        val result = getFiles(dir)
        assertEquals(3, result.size)
    }

    @Test
    fun `空目录返回空列表`(@TempDir dir: File) {
        val result = getFiles(dir)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `单个 yml 文件作为参数`(@TempDir dir: File) {
        val file = File(dir, "single.yml").apply { createNewFile() }
        val result = getFiles(file)
        assertEquals(1, result.size)
        assertEquals(file, result[0])
    }

    @Test
    fun `单个非 yml 非 js 文件返回空`(@TempDir dir: File) {
        val file = File(dir, "readme.txt").apply { createNewFile() }
        val result = getFiles(file)
        assertTrue(result.isEmpty())
    }
}
