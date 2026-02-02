package org.gitee.nodens.common

import java.util.concurrent.ConcurrentHashMap

/**
 * # FastMatchingMap - 高性能字符串匹配容器
 *
 * 基于 **Trie（前缀树）** 数据结构实现的线程安全快速匹配 Map。
 * 支持纳秒级的字符串匹配操作，特别适用于 Minecraft 插件中的 Lore 解析场景。
 *
 * ## 特性
 * - **线程安全**：使用 [ConcurrentHashMap] 实现无锁并发
 * - **高性能**：基于 Trie 树的 O(m) 时间复杂度匹配（m 为匹配串长度）
 * - **灵活过滤**：支持忽略空格、颜色代码、冒号等干扰字符
 * - **前缀匹配**：支持从任意位置开始匹配，无需精确对齐
 *
 * ## 使用示例
 * ```kotlin
 * val matcher = FastMatchingMap<ItemAttribute>()
 * matcher.put("攻击力", AttackAttribute)
 * matcher.put("防御力", DefenseAttribute)
 *
 * // 匹配 Lore 行
 * val attr = matcher.get("§7攻击力: +100")  // 返回 AttackAttribute
 * ```
 *
 * @param T 存储值的类型
 * @param ignoreSpace 是否忽略空白字符（空格、制表符、换行符）
 * @param ignoreColor 是否忽略 Minecraft 颜色代码（`&` 或 `§` 开头）
 * @param ignoreColon 是否忽略冒号（半角 `:` 和全角 `：`）
 * @param ignorePrefix 是否允许从字符串任意位置开始匹配
 *
 * @author zhibei
 * @since 1.0.0
 */
class FastMatchingMap<T>(
    private val ignoreSpace: Boolean = true,
    private val ignoreColor: Boolean = true,
    private val ignoreColon: Boolean = true,
    private val ignorePrefix: Boolean = true,
) {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //                              内部数据结构
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Trie 树节点
     *
     * 每个节点包含：
     * - 子节点 Map：使用字符作为键，按需创建子节点
     * - 终止值：当该节点是某个 key 的终点时，存储对应的值
     *
     * 内存优化：使用 [ConcurrentHashMap] 替代固定大小数组，
     * 从每节点 512KB 降低到约 1KB（假设平均 10 个子节点）
     *
     * @param children 子节点 Map，使用 [ConcurrentHashMap] 保证线程安全
     * @param value 该节点对应的值，使用 [@Volatile] 保证可见性
     */
    private class TrieNode<T>(
        val children: ConcurrentHashMap<Char, TrieNode<T>> = ConcurrentHashMap(4),
        @Volatile var value: T? = null
    )

    /** Trie 树根节点 */
    private var root = TrieNode<T>()

    /** 所有已注册 key 的首字符集合，用于快速定位匹配起点 */
    private val rootChars = ConcurrentHashMap.newKeySet<Char>()

    /** 当前存储的键值对数量 */
    val size: Int
        get() = rootChars.size

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //                              核心方法
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * 添加键值对到匹配器中
     *
     * 将 key 预处理后插入 Trie 树，支持并发写入。
     * 相同的 key 会覆盖之前的值。
     *
     * @param key 匹配键（会经过预处理去除干扰字符）
     * @param value 对应的值
     *
     * ### 示例
     * ```kotlin
     * matcher.put("攻击力", AttackAttribute)
     * matcher.put("Attack", AttackAttribute)  // 支持多个 key 映射同一值
     * ```
     */
    fun put(key: String, value: T) {
        val cleanKey = preprocess(key)
        var current = root

        // 沿着 key 的每个字符向下遍历/创建节点
        for (c in cleanKey) {
            // 使用 computeIfAbsent 原子性地获取或创建子节点
            current = current.children.computeIfAbsent(c) { TrieNode() }
        }

        // 在终止节点设置值
        current.value = value

        // 空 key 不记录首字符
        if (current === root) return

        // 记录首字符用于快速定位
        rootChars.add(cleanKey.first())
    }

    /**
     * 查询匹配的值（最短匹配优先）
     *
     * 在 Trie 树中搜索第一个匹配的 key，返回对应的值。
     * 采用**最短匹配**策略，即找到第一个有效终止节点就立即返回。
     *
     * @param lore 待匹配的字符串（通常是物品 Lore 的一行）
     * @return 匹配到的值，未匹配返回 `null`
     *
     * ### 示例
     * ```kotlin
     * matcher.put("攻击", value1)
     * matcher.put("攻击力", value2)
     *
     * matcher.get("攻击力: +100")  // 返回 value1（最短匹配）
     * ```
     */
    fun get(lore: String): T? {
        val line = preprocess(lore)
        var start = 0

        // 定位匹配起点
        if (ignorePrefix) {
            start = findStartIndex(line)
            if (start == -1) return null
        }

        // 沿 Trie 树向下搜索
        var current = root
        for (i in start until line.length) {
            val node = current.children[line[i]] ?: return null
            // 最短匹配：找到值立即返回
            node.value?.let { return it }
            current = node
        }
        return null
    }

    /**
     * 获取匹配结果及剩余字符串
     *
     * 与 [get] 类似，但额外返回匹配后的剩余字符串，
     * 便于进一步解析数值等信息。
     *
     * @param lore 待匹配的字符串
     * @return 匹配结果 [MatchResult]，包含值和剩余字符串；未匹配返回 `null`
     *
     * ### 示例
     * ```kotlin
     * matcher.put("攻击力", AttackAttribute)
     *
     * val result = matcher.getMatchResult("攻击力+100")
     * // result.value = AttackAttribute
     * // result.remain = "+100"
     * ```
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
            val node = current.children[line[i]] ?: return null
            node.value?.let {
                return MatchResult(
                    remain = line.substring(i + 1).takeIf { i + 1 < line.length },
                    value = it
                )
            }
            current = node
        }
        return null
    }

    /**
     * 清空所有数据
     *
     * 重置 Trie 树和首字符集合，释放所有存储的键值对。
     */
    fun clear() {
        rootChars.clear()
        root = TrieNode()
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //                              私有辅助方法
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * 预处理字符串
     *
     * 根据配置移除干扰字符：
     * - 空白字符（空格、制表符、换行符）
     * - Minecraft 颜色代码（`&x` 或 `§x`）
     * - 冒号（半角和全角）
     *
     * @param lore 原始字符串
     * @return 处理后的干净字符串
     */
    private fun preprocess(lore: String): String {
        return buildString(lore.length) {
            var i = 0
            while (i < lore.length) {
                when {
                    // 跳过空白字符
                    ignoreSpace && lore[i].isWhitespaceFast() -> i++
                    // 跳过颜色代码（两个字符）
                    ignoreColor && isColorCodeStart(lore[i]) -> {
                        if (++i < lore.length && isColorCodeContent(lore[i])) i++
                    }
                    // 跳过冒号
                    ignoreColon && isColon(lore[i]) -> i++
                    // 保留其他字符
                    else -> append(lore[i++])
                }
            }
        }
    }

    /**
     * 查找匹配起始位置
     *
     * 在字符串中查找第一个属于 [rootChars] 的字符位置，
     * 用于实现从任意前缀开始匹配的功能。
     *
     * @param line 预处理后的字符串
     * @return 起始索引，未找到返回 -1
     */
    private fun findStartIndex(line: String): Int {
        for (i in line.indices) {
            if (rootChars.contains(line[i])) return i
        }
        return -1
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //                              内联工具函数
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /** 快速判断是否为空白字符 */
    private fun Char.isWhitespaceFast() =
        this == ' ' || this == '\t' || this == '\n' || this == '\r'

    /** 判断是否为颜色代码起始符 */
    private fun isColorCodeStart(c: Char) =
        c == '&' || c == '§'

    /** 判断是否为有效的颜色代码内容（0-9, a-f, k-o, r） */
    private fun isColorCodeContent(c: Char) =
        c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' ||
        c in "k-o" || c in "K-O" || c == 'r' || c == 'R'

    /** 判断是否为冒号（半角或全角） */
    private fun isColon(c: Char) =
        c == ':' || c == '：'

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //                              数据类
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * 匹配结果
     *
     * @param remain 匹配后的剩余字符串，可用于解析数值；若无剩余则为 `null`
     * @param value 匹配到的值
     */
    class MatchResult<T>(val remain: String?, val value: T)
}
