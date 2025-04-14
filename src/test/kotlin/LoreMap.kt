/**
 * 初始化一个LoreMap
 *
 * @param ignoreSpace 是否忽略空格
 * @param ignoreColor 是否忽略颜色代码
 * @param ignorePrefix 是否允许从任意前缀开始匹配
 */
class LoreMap<T>(
    private val ignoreSpace: Boolean = true,
    private val ignoreColor: Boolean = true,
    private val ignoreColon: Boolean = true,
    private val ignorePrefix: Boolean = false,
) {
    companion object {
        private val SPACE_REGEX = Regex("\\s")
        private val COLOR_REGEX = Regex("(?i)[&§][0-9a-fk-or]")
        private val COLON_REGEX = Regex("[:：]")
    }

    private val root = TrieNode<T>()

    /**
     * 预处理字符串的通用方法
     */
    private fun preprocess(lore: String): String {
        var processed = lore
        if (ignoreSpace) processed = processed.replace(SPACE_REGEX, "")
        if (ignoreColor) processed = processed.replace(COLOR_REGEX, "")
        if (ignoreColon) processed = processed.replace(COLON_REGEX, "")
        return processed
    }

    /**
     * 向LoreMap中放入lore和对应的对象
     */
    fun put(lore: String, value: T) {
        val line = preprocess(lore)
        var current = root
        for (c in line) {
            current = current.children.getOrPut(c) { TrieNode() }
        }
        current.value = value
    }

    /**
     * 查询lore对应的对象（最短匹配）
     */
    fun get(lore: String): T? {
        val line = preprocess(lore)
        var startIndex = 0

        if (ignorePrefix) {
            startIndex = line.indexOfFirst { root.children.containsKey(it) }
            if (startIndex == -1) return null
        }

        var current = root
        for (i in startIndex until line.length) {
            current = current.children[line[i]] ?: return null
            current.value?.let { return it }
        }
        return null
    }

    /**
     * 获取匹配结果及剩余字符串
     */
    fun getMatchResult(lore: String): MatchResult<T>? {
        val line = preprocess(lore)
        var startIndex = 0

        if (ignorePrefix) {
            startIndex = line.indexOfFirst { root.children.containsKey(it) }
            if (startIndex == -1) return null
        }

        var current = root
        for (i in startIndex until line.length) {
            current = current.children[line[i]] ?: return null
            current.value?.let {
                val remain = line.substring(i + 1).takeIf { i + 1 < line.length }
                return MatchResult(remain, it)
            }
        }
        return null
    }

    fun clear() {
        root.children.clear()
    }

    private class TrieNode<T> {
        val children = HashMap<Char, TrieNode<T>>()
        var value: T? = null
    }

    class MatchResult<T>(val remain: String?, val value: T)
}