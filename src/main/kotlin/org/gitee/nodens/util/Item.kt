package org.gitee.nodens.util

import com.github.benmanes.caffeine.cache.Caffeine
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.module.item.*
import org.gitee.nodens.module.item.drop.DropManager
import org.gitee.nodens.module.item.generator.NormalGenerator.generate
import taboolib.module.nms.getItemTag
import taboolib.platform.util.isAir
import taboolib.platform.util.isNotAir
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

const val CONTEXT_TAG = "NODENS_CONTEXT"
/** 出售价格标签 */
const val SELL_TAG = "NODENS@SELL"
/** 出售价格键 */
const val SELL = "sell"
/** 品质标签 */
const val QUALITY_TAG = "NODENS@QUALITY"
/** 品质键 */
const val QUALITY = "quality"
/** 耐久度标签 */
const val DURABILITY_TAG = "NODENS@DURABILITY"
/** 耐久度键 */
const val DURABILITY = "durability"

/**
 * NormalContext 缓存
 * - 使用 ItemStack 的弱引用作为 key，避免内存泄漏
 * - 500ms 访问后过期，提高缓存命中率
 * - 最大 2000 条缓存
 * - 启用统计以监控缓存效率
 */
private val contextCache = Caffeine.newBuilder()
    .weakKeys()
    .expireAfterAccess(500, TimeUnit.MILLISECONDS)
    .maximumSize(2000)
    .recordStats()
    .build<ItemStack, NormalContext>()

/**
 * 获取物品的 [NormalContext]
 *
 * 从物品的 NBT 标签中读取并反序列化上下文数据，支持缓存以提高性能。
 *
 * @receiver 要获取上下文的物品
 * @return 物品的上下文，如果物品为空或没有上下文数据则返回 `null`
 * @see NormalContext
 */
@Suppress("UNCHECKED_CAST")
fun ItemStack.context(): NormalContext? {
    if (isAir()) return null
    // 尝试从缓存获取
    contextCache.getIfPresent(this)?.let { return it }
    // 缓存未命中，执行完整解析
    val byteArray = getItemTag()[CONTEXT_TAG]?.asByteArray() ?: return null
    val context = ContextSerializer.deserialize(decompressToBytes(byteArray))
    // 存入缓存
    contextCache.put(this, context)
    return context
}

/**
 * 序列化 [NormalContext] 为压缩后的字节数组
 *
 * @receiver 要序列化的上下文对象
 * @return 压缩后的字节数组
 * @see ContextSerializer.serialize
 */
fun NormalContext.toByteArray(): ByteArray {
    return compress(ContextSerializer.serialize(this))
}

/**
 * 修改物品的 [NormalContext]
 *
 * 获取物品的上下文，应用修改后保存回物品的 NBT 标签。
 *
 * @receiver 要修改上下文的物品
 * @param consumer 上下文修改器
 * @see NormalContext
 */
fun ItemStack.modifyContext(consumer: Consumer<NormalContext>) {
    val context = context()?.also { consumer.accept(it) } ?: return
    val tag = getItemTag()
    tag[CONTEXT_TAG] = context.toByteArray()
    tag.saveTo(this)
    // 修改后使缓存失效
    contextCache.invalidate(this)
}

/**
 * 将任意值转换为 [Variable]
 *
 * 支持的类型：
 * - `null` -> [NullVariable]
 * - [Variable] -> 原样返回
 * - [List] -> [ArrayVariable]
 * - [Map] -> [MapVariable]
 * - 基本类型 -> 对应的 Variable 类型
 *
 * @receiver 要转换的值
 * @return 转换后的 [Variable] 对象
 * @throws IllegalStateException 如果类型不支持且没有注册转换器
 * @see Variable
 * @see VariableRegistry.convert
 */
@Suppress("UNCHECKED_CAST")
fun Any?.toVariable(): Variable<*> {
    return when (this) {
        null -> NullVariable(null)
        is Variable<*> -> this
        is List<*> -> ArrayVariable(this.map { it.toVariable() })
        is Map<*, *> -> MapVariable(this.mapValues { it.value.toVariable() } as Map<String, Variable<*>>)
        is Byte -> ByteVariable(this)
        is Short -> ShortVariable(this)
        is Int -> IntVariable(this)
        is Long -> LongVariable(this)
        is Float -> FloatVariable(this)
        is Double -> DoubleVariable(this)
        is Char -> CharVariable(this)
        is String -> StringVariable(this)
        is Boolean -> BooleanVariable(this)
        else -> VariableRegistry.convert(this) ?: error("not supported variable type: ${this::class.qualifiedName}")
    }
}

/**
 * 获取物品对应的 [ItemConfig] 配置
 *
 * @receiver 要获取配置的物品
 * @return 物品配置，如果物品没有上下文或配置不存在则返回 `null`
 * @see ItemConfig
 * @see ItemManager.itemConfigs
 */
fun ItemStack.getConfig(): ItemConfig? {
    val context = context() ?: return null
    return ItemManager.itemConfigs[context.key]
}

/**
 * 给予玩家指定物品
 *
 * 生成物品并添加到玩家背包，如果背包已满则掉落到地上。
 *
 * @receiver 接收物品的玩家
 * @param item 物品配置的 key
 * @param amount 物品数量
 * @param map 额外的变量映射
 * @return 生成的物品，如果物品配置不存在则返回 `null`
 * @see ItemManager.getItemConfig
 * @see DropManager.drop
 */
fun Player.giveItems(item: String, amount: Int, map: Map<String, Any> = emptyMap()): ItemStack? {
    val item = ItemManager.getItemConfig(item) ?: return null
    val itemStack = generate(item, amount, this, map, true)

    if (itemStack.isNotAir()) {
        val preAmount = itemStack.amount

        inventory.addItem(itemStack).values.forEach {
            DropManager.drop(this, location, it)
            world.dropItem(location, it)
        }
        itemStack.amount = preAmount
    }

    return itemStack
}