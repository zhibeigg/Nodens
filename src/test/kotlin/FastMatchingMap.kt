import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReferenceArray
import javax.swing.text.html.InlineView

/**
 * 初始化一个LoreMap（线程安全版）
 *
 * @param ignoreSpace 是否忽略空格
 * @param ignoreColor 是否忽略颜色代码
 * @param ignoreColon 是否忽略冒号
 * @param ignorePrefix 是否允许从任意前缀开始匹配
 */
class FastLoreMap<T>(
    private val ignoreSpace: Boolean = true,
    private val ignoreColor: Boolean = true,
    private val ignoreColon: Boolean = true,
    private val ignorePrefix: Boolean = false,
) {
    private class TrieNode<T>(
        val children: AtomicReferenceArray<TrieNode<T>?> = AtomicReferenceArray(65536),
        @Volatile var value: T? = null
    )

    private val root = TrieNode<T>()
    private val rootChars = ConcurrentHashMap.newKeySet<Char>()

    /**
     * 预处理字符串的通用方法
     */
    private fun preprocess(lore: String): String {
        return buildString(lore.length) {
            var i = 0
            while (i < lore.length) {
                when {
                    ignoreSpace && lore[i].isWhitespaceFast() -> i++
                    ignoreColor && isColorCodeStart(lore[i]) -> {
                        if (++i < lore.length && isColorCodeContent(lore[i])) i++
                    }
                    ignoreColon && isColon(lore[i]) -> i++
                    else -> append(lore[i++])
                }
            }
        }
    }

    /**
     * 向LoreMap中放入lore和对应的对象（线程安全）
     */
    fun put(lore: String, value: T) {
        val cleanKey = preprocess(lore)
        // 当前指针为根目录
        var current = root

        // 遍历当前识别Key
        for (c in cleanKey) {
            // 获取当前遍历到的 char 的 UTF-16 代码单元
            val code = c.code
            // 获取子目录
            var node = current.children[code]

            // 如果子目录为空
            if (node == null) {
                // 创建子目录
                node = TrieNode()
                // 设置子目录
                current.children.set(code, node)
            }

            // 指针为子目录
            current = node
        }

        // 设置当前指针的绑定对象
        current.value = value
        // 如果当前指针还在根目录 return
        if (current === root) return
        // 添加定位识别头
        rootChars.add(cleanKey.first())
    }

    /**
     * 查询lore对应的对象（最短匹配，线程安全）
     */
    fun get(lore: String): T? {
        val line = preprocess(lore)
        var start = 0

        if (ignorePrefix) {
            start = findStartIndex(line)
            if (start == -1) return null
        }

        var current = root
        for (i in start until line.length) {
            val node = current.children[line[i].code] ?: return null
            node.value?.let { return it }
            current = node
        }
        return null
    }

    /**
     * 获取匹配结果及剩余字符串（线程安全）
     */
    fun getMatchResult(lore: String): MatchResult<T>? {
        val line = preprocess(lore)
        var start = 0

        if (ignorePrefix) {
            start = findStartIndex(line)
            if (start == -1) return null
        }

        var current = root
        for (i in start until line.length) {
            val node = current.children[line[i].code] ?: return null
            node.value?.let {
                return MatchResult(
                    line.substring(i + 1).takeIf { i + 1 < line.length },
                    it
                )
            }
            current = node
        }
        return null
    }

    private fun findStartIndex(line: String): Int {
        for (i in line.indices) {
            if (rootChars.contains(line[i])) return i
        }
        return -1
    }

    // 内联工具函数
    private inline fun Char.isWhitespaceFast() = this == ' ' || this == '\t' || this == '\n' || this == '\r'
    private inline fun isColorCodeStart(c: Char) = c == '&' || c == '§'
    private inline fun isColorCodeContent(c: Char) = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' || c in "k-o" || c in "K-O" || c == 'r' || c == 'R'
    private inline fun isColon(c: Char) = c == ':' || c == '：'

    class MatchResult<T>(val remain: String?, val value: T)
}