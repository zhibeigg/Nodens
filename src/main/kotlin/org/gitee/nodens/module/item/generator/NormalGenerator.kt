package org.gitee.nodens.module.item.generator

import kotlinx.serialization.json.Json
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

    override fun generate(itemConfig: ItemConfig, amount: Int, player: Player?, map: Map<String, Any>): ItemStack {
        val sender = player?.let { adaptPlayer(it) } ?: console()
        val context = NormalContext(itemConfig.key, hashMapOf(), itemConfig.hashCode)

        // 预置参数
        itemConfig.variables.forEach {
            if (map.containsKey(it.key)) return@forEach
            context.variable[it.key] = it.getVariable(sender, itemConfig, context)
        }
        // 生成出售价格
        context.variable[SELL_TAG] = (itemConfig.sell?.let { eval(sender, itemConfig, context, it).cdouble } ?: 0.0).toVariable()
        // 生成最大耐久值
        context.variable[DURABILITY_TAG] = (itemConfig.durability?.let { eval(sender, itemConfig, context, it).cint } ?: 0.0).toVariable()
        // 覆盖自定义数值
        context.variable.putAll(map.mapValues { it.value.toVariable() })
        val parser = parse(sender, itemConfig, context, itemConfig.lore + itemConfig.name)

        val builder = ItemBuilder(itemConfig.material)
        builder.name = parser.last()
        builder.amount = amount
        parser.dropLast(1).forEach {
            builder.lore += it
        }
        itemConfig.itemFlags.forEach {
            builder.flags += it.get() ?: return@forEach
        }
        itemConfig.enchantments.forEach {
            builder.enchants[it.key.get() ?: return@forEach] = it.value?.let { level ->
                parse(sender, itemConfig, context, level).cint
            } ?: 1
        }
        builder.isUnbreakable = itemConfig.isUnBreakable
        builder.colored()
        val durability = context.variable["durability"]?.value
        if (durability == null) {
            context.variable["durability"] = context.variable[DURABILITY_TAG]!!
        }
        if (!itemConfig.isUnBreakable) {
            val max = context.variable[DURABILITY_TAG]!!.value.cint
            if (max != 0) {
                builder.damage = (builder.material.maxDurability.cdouble * (1 - context.variable["durability"]!!.value.cdouble / max.cdouble)).cint
            }
        }
        builder.finishing = {
            val tag = it.getItemTag()
            tag[CONTEXT_TAG] = Json.encodeToString(context)
            tag["durability"] = context.variable["durability"]!!.value.cint
            tag.saveTo(it)
            it.replaceLore(
                mapOf(
                    "{CombatPower}" to ceil(
                        AttributeManager.getCombatPower(
                            *it.getItemAttribute().toTypedArray()
                        )
                    ).toString(),
                    "{MaxDurability}" to context.variable[DURABILITY_TAG]!!.value.cint.toString(),
                    "{Sell}" to ceil(context.variable[SELL_TAG]!!.value.cdouble).toString(),
                )
            )
        }
        val event = NodensItemGenerateEvent(player, itemConfig, context, builder.build())
        event.call()
        return event.item
    }

    private fun parse(sender: ProxyCommandSender, itemConfig: ItemConfig, context: NormalContext, list: List<String>): List<String> {
        return KetherFunction.parse(
            list,
            ScriptOptions.builder()
                .namespace(namespace = nodensEnvironmentNamespaces)
                .sender(sender)
                .sandbox(false)
                .set("item", itemConfig)
                .set("context", context)
                .build()
        )
    }

    private fun parse(sender: ProxyCommandSender, itemConfig: ItemConfig, context: NormalContext, string: String): String {
        return KetherFunction.parse(
            string,
            ScriptOptions.builder()
                .namespace(namespace = nodensEnvironmentNamespaces)
                .sender(sender)
                .sandbox(false)
                .set("item", itemConfig)
                .set("context", context)
                .build()
        )
    }

    private fun eval(sender: ProxyCommandSender, itemConfig: ItemConfig, context: NormalContext, string: String): Any? {
        return KetherShell.eval(
            string,
            ScriptOptions.builder()
                .namespace(namespace = nodensEnvironmentNamespaces)
                .sender(sender)
                .sandbox(false)
                .set("item", itemConfig)
                .set("context", context)
                .build()
        ).orNull()
    }

    /**
     * 生成物品的variables数据
     * @param sender 执行者
     * @param itemConfig 物品配置
     * */
    private fun ItemConfig.Variable.getVariable(sender: ProxyCommandSender, itemConfig: ItemConfig, context: NormalContext): Variable<*> {
        val any = try {
            KetherShell.eval(
                action,
                ScriptOptions.builder()
                    .namespace(namespace = nodensEnvironmentNamespaces)
                    .sender(sender)
                    .sandbox(false)
                    .set("item", itemConfig)
                    .set("context", context)
                    .build()
            ).orNull() ?: error("not variable $key")
        } catch (e: Throwable) {
            e.printKetherErrorMessage()
            "none-error"
        }
        return any.toVariable()
    }

    override fun update(player: Player?, itemStack: ItemStack): ItemStack? {
        val context = itemStack.context<NormalContext>() ?: return null
        val config = ItemManager.getItemConfig(context.key) ?: return null
        val new = generate(config, itemStack.amount, player, context.variable)
        val event = NodensItemUpdateEvents.Pre(itemStack, new)
        return if (event.call()) {
            event.new
        } else {
            event.old
        }
    }
}