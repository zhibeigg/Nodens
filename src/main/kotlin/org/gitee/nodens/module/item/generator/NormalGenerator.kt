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

    override fun generate(itemConfig: ItemConfig, amount: Int, player: Player?, map: Map<String, Any>, callEvent: Boolean): ItemStack {
        val sender = player?.let { adaptPlayer(it) } ?: console()
        val context = NormalContext(itemConfig.key, hashMapOf(), itemConfig.hashCode)
        val extendMap = mutableMapOf<String, Any?>()

        // 预置参数
        itemConfig.variables.forEach {
            if (map.containsKey(it.key)) return@forEach
            context[it.key] = it.getVariable(sender, itemConfig, context, extendMap)
        }
        // 覆盖自定义数值
        context.putAll(map)
        // 生成品质
        val quality = itemConfig.quality?.let { eval(sender, itemConfig, context, it, extendMap).cint } ?: 0
        extendMap[QUALITY] = quality
        // 生成出售价格
        val sell = itemConfig.sell?.let { eval(sender, itemConfig, context, it, extendMap).cdouble } ?: 0.0
        context[SELL_TAG] = sell
        extendMap[SELL] = quality
        // 生成最大耐久值
        val maxDurability = itemConfig.durability?.let { eval(sender, itemConfig, context, it, extendMap).cint } ?: 0.0
        context[DURABILITY_TAG] = maxDurability
        extendMap[DURABILITY] = maxDurability
        val parser = parse(sender, itemConfig, context, itemConfig.lore + itemConfig.name, extendMap)

        val builder = ItemBuilder(itemConfig.material)
        builder.name = parser.last()
        builder.amount = amount
        // 生成 lore
        parser.dropLast(1).forEach { line ->
            // 将含有 *0* 的行删除
            // 为了删除算法生成为 0 的属性
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
        val durability = context[DURABILITY]
        if (durability == null) {
            context[DURABILITY] = context[DURABILITY_TAG]!!
        }
        if (!itemConfig.isUnBreakable) {
            val max = context[DURABILITY_TAG]!!.cint
            if (max != 0) {
                builder.damage = (builder.material.maxDurability.cdouble * (1 - context[DURABILITY]!!.cdouble / max.cdouble)).cint
            }
        }
        builder.finishing = {
            val tag = it.getItemTag()
            tag[CONTEXT_TAG] = compress(VariableRegistry.json.encodeToString(context))
            tag[DURABILITY] = context[DURABILITY]!!.cint
            tag[QUALITY] = quality
            itemConfig.nbt?.forEach { (key, value) ->
                tag[key] = value
            }
            tag.saveTo(it)
            it.replaceLore(
                mapOf(
                    "{CombatPower}" to ceil(
                        AttributeManager.getCombatPower(
                            *it.getItemAttribute().toTypedArray()
                        )
                    ).toString(),
                    "{MaxDurability}" to context[DURABILITY_TAG]!!.cint.toString(),
                    "{Sell}" to ceil(context[SELL_TAG]!!.cdouble).toString(),
                )
            )
        }

        return if (callEvent) {
            val event = NodensItemGenerateEvent(player, itemConfig, context, builder.build())
            event.call()
            event.item
        } else {
            builder.build()
        }
    }

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