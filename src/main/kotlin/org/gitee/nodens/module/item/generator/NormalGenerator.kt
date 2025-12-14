package org.gitee.nodens.module.item.generator

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.events.item.NodensItemGenerateEvent
import org.gitee.nodens.api.events.item.NodensItemUpdateEvents
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.getItemAttribute
import org.gitee.nodens.module.item.*
import org.gitee.nodens.util.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.console
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.module.kether.*
import taboolib.module.nms.getItemTag
import taboolib.platform.util.ItemBuilder
import taboolib.platform.util.replaceLore
import kotlin.math.ceil

object NormalGenerator: IItemGenerator {

    private val REMOVE_REGEX = Regex("\\*[0-9.]+\\*")

    /**
     * 物品生成流程:
     * 1. 初始化上下文 -> 2. 计算变量 -> 3. 生成属性(品质/售价/耐久)
     * -> 4. 解析Lore/Name -> 5. 构建物品 -> 6. 保存NBT -> 7. 触发事件
     */
    override fun generate(itemConfig: ItemConfig, amount: Int, player: Player?, map: Map<String, Any>, callEvent: Boolean): ItemStack {
        val sender = player?.let { adaptPlayer(it) } ?: console()
        val context = NormalContext(itemConfig.key, hashMapOf(), itemConfig.hashCode)
        val extendMap = mutableMapOf<String, Any?>()

        // ========== 阶段1: 变量初始化 ==========
        // 从配置的 variables 中计算预置参数
        itemConfig.variables.forEach {
            if (map.containsKey(it.key)) return@forEach
            context[it.key] = it.getVariable(sender, itemConfig, context, emptyMap())
        }

        // 用传入的 map 覆盖自定义数值
        context.putAll(map)

        // ========== 阶段2: 生成核心属性 ==========
        // 品质
        if (!map.containsKey(QUALITY_TAG)) {
            val quality = itemConfig.quality?.let { eval(sender, itemConfig, context, it, extendMap).cint } ?: 0
            context[QUALITY_TAG] = quality
        }
        extendMap[QUALITY] = context[QUALITY_TAG]

        // 出售价格
        if (!map.containsKey(SELL_TAG)) {
            val sell = itemConfig.sell?.let { eval(sender, itemConfig, context, it, extendMap).cdouble } ?: 0.0
            context[SELL_TAG] = sell
        }
        extendMap[SELL] = context[SELL_TAG]

        // 最大耐久值
        if (!map.containsKey(DURABILITY_TAG)) {
            val maxDurability = itemConfig.durability?.let { eval(sender, itemConfig, context, it, extendMap).cint } ?: 0.0
            context[DURABILITY_TAG] = maxDurability
        }
        extendMap[DURABILITY] = context[DURABILITY_TAG]

        // ========== 阶段3: 解析Lore和Name ==========
        val parser = parse(sender, itemConfig, context, itemConfig.lore + itemConfig.name, extendMap)

        // ========== 阶段4: 构建物品 ==========
        val builder = ItemBuilder(itemConfig.material)
        builder.name = parser.last()
        builder.amount = amount

        // 处理Lore: 移除 *0* 标记的行(属性值为0的行)
        parser.dropLast(1).forEach { line ->
            var newLine: String? = line
            REMOVE_REGEX.find(line)?.let { matchResult ->
                val number = matchResult.value.let { value -> value.substring(1, value.length - 2) }.cdouble
                newLine = if (number == 0.0) {
                    null
                } else {
                    line.replace(matchResult.value, number.toString())
                }
            }
            if (newLine != null) {
                builder.lore += newLine
            }
        }

        // 设置物品标志、附魔、不可破坏
        itemConfig.itemFlags.forEach {
            builder.flags += it.get() ?: return@forEach
        }
        itemConfig.enchantments.forEach {
            builder.enchants[it.key.get() ?: return@forEach] = it.value?.let { level ->
                parse(sender, itemConfig, context, level, extendMap).cint
            } ?: 1
        }
        builder.isUnbreakable = itemConfig.isUnBreakable
        builder.colored()

        // 初始化当前耐久为最大耐久
        val durability = context[DURABILITY]
        if (durability == null) {
            context[DURABILITY] = context[DURABILITY_TAG]!!
        }

        // 根据耐久比例设置物品损坏度显示
        if (!itemConfig.isUnBreakable) {
            val max = context[DURABILITY_TAG]!!.cint
            if (max != 0) {
                builder.damage = (builder.material.maxDurability.cdouble * (1 - context[DURABILITY]!!.cdouble / max.cdouble)).cint
            }
        }

        // ========== 阶段5: 保存NBT数据 ==========
        builder.finishing = {
            val tag = it.getItemTag()
            // 保存上下文(序列化压缩)
            tag[CONTEXT_TAG] = compress(VariableRegistry.json.encodeToString(context))
            tag[DURABILITY] = context[DURABILITY]!!.cint
            tag[QUALITY] = context[QUALITY_TAG]!!.cint
            // 保存自定义NBT
            itemConfig.nbt?.forEach { (key, value) ->
                tag[key] = value
            }
            tag.saveTo(it)
            // 替换Lore中的占位符
            it.replaceLore(
                mapOf(
                    "{CombatPower}" to ceil(
                        AttributeManager.getCombatPower(
                            *it.getItemAttribute().toTypedArray()
                        )
                    ).toString(),
                    "{MaxDurability}" to context[DURABILITY_TAG]!!.cint.toString(),
                    "{Sell}" to ceil(context[SELL_TAG]!!.cdouble).toString()
                )
            )
        }

        // ========== 阶段6: 触发事件并返回 ==========
        return if (callEvent) {
            val event = NodensItemGenerateEvent(player, itemConfig, context, builder.build())
            event.call()
            event.item
        } else {
            builder.build()
        }
    }

    /** 批量解析Kether表达式(用于Lore) */
    private fun parse(sender: ProxyCommandSender, itemConfig: ItemConfig, context: NormalContext, list: List<String>, map: Map<String, Any?>): List<String> {
        return KetherFunction.parse(
            list,
            ScriptOptions.builder()
                .namespace(namespace = nodensEnvironmentNamespaces)
                .sender(sender)
                .sandbox(false)
                .set("item", itemConfig)
                .set("context", context)
                .vars(map)
                .build()
        )
    }

    /** 解析单个Kether表达式 */
    private fun parse(sender: ProxyCommandSender, itemConfig: ItemConfig, context: NormalContext, string: String, map: Map<String, Any?>): String {
        return KetherFunction.parse(
            string,
            ScriptOptions.builder()
                .namespace(namespace = nodensEnvironmentNamespaces)
                .sender(sender)
                .sandbox(false)
                .set("item", itemConfig)
                .set("context", context)
                .vars(map)
                .build()
        )
    }

    /** 执行Kether脚本并返回结果(用于计算数值) */
    private fun eval(sender: ProxyCommandSender, itemConfig: ItemConfig, context: NormalContext, string: String, map: Map<String, Any?>): Any? {
        return KetherShell.eval(
            string,
            ScriptOptions.builder()
                .namespace(namespace = nodensEnvironmentNamespaces)
                .sender(sender)
                .sandbox(false)
                .set("item", itemConfig)
                .set("context", context)
                .vars(map)
                .build()
        ).orNull()
    }

    /**
     * 生成物品的variables数据
     * @param sender 执行者
     * @param itemConfig 物品配置
     * */
    private fun ItemConfig.Variable.getVariable(sender: ProxyCommandSender, itemConfig: ItemConfig, context: NormalContext, map: Map<String, Any?>): Any {
        val any = try {
            KetherShell.eval(
                action,
                ScriptOptions.builder()
                    .namespace(namespace = nodensEnvironmentNamespaces)
                    .sender(sender)
                    .sandbox(false)
                    .set("item", itemConfig)
                    .set("context", context)
                    .vars(map)
                    .build()
            ).orNull() ?: error("not variable $key")
        } catch (e: Throwable) {
            e.printKetherErrorMessage()
            "none-error"
        }
        return any
    }

    /**
     * 更新物品: 基于当前context重新生成物品，保留耐久值
     * 用于配置变更后刷新已有物品
     */
    override fun update(player: Player?, itemStack: ItemStack): ItemStack? {
        val context = itemStack.context() ?: return null
        val config = ItemManager.getItemConfig(context.key) ?: return null
        val new = generate(config, itemStack.amount, player, context.sourceMap(), false)
        val tag = new.getItemTag()
        tag["durability"] = context["durability"]!!.cint
        tag.saveTo(new)
        val event = NodensItemUpdateEvents.Pre(itemStack, new)
        return if (event.call()) {
            event.new
        } else {
            event.old
        }
    }
}