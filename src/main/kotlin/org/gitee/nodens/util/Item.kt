package org.gitee.nodens.util

import kotlinx.serialization.json.Json
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.module.item.*
import org.gitee.nodens.module.item.drop.DropManager
import org.gitee.nodens.module.item.generator.NormalGenerator.generate
import taboolib.common.platform.function.info
import taboolib.module.nms.ItemTagData
import taboolib.module.nms.getItemTag
import taboolib.platform.util.isAir
import taboolib.platform.util.isNotAir
import java.util.function.Consumer
import java.util.function.Function

const val CONTEXT_TAG = "NODENS_CONTEXT"
const val SELL_TAG = "NODENS@SELL"
const val DURABILITY_TAG = "NODENS@DURABILITY"

@Suppress("UNCHECKED_CAST")
inline fun <reified T: IItemContext> ItemStack.context(): T? {
    if (isAir()) return null
    return Json.decodeFromString<T>(getItemTag()[CONTEXT_TAG]?.asString() ?: return null)
}

@Suppress("UNCHECKED_CAST")
fun ItemStack.context(): NormalContext? {
    if (isAir()) return null
    return Json.decodeFromString<NormalContext>(getItemTag()[CONTEXT_TAG]?.asString() ?: return null)
}

fun ItemStack.modifyContext(consumer: Consumer<NormalContext>) {
    val context = context()?.also { consumer.accept(it) } ?: return
    val tag = getItemTag()
    tag[CONTEXT_TAG] = Json.encodeToString(context)
    tag.saveTo(this)
}

fun Any.toVariable(): Variable<*> {
    return when (this) {
        is Variable<*> -> this
        is Byte -> ByteVariable(this)
        is Short -> ShortVariable(this)
        is Int -> IntVariable(this)
        is Long -> LongVariable(this)
        is Float -> FloatVariable(this)
        is Double -> DoubleVariable(this)
        is Char -> CharVariable(this)
        is String -> StringVariable(this)
        is Boolean -> BooleanVariable(this)
        else -> error("not supported variable type: ${this::class.qualifiedName}")
    }
}

fun ItemStack.getConfig(): ItemConfig? {
    val context = context<NormalContext>() ?: return null
    return ItemManager.itemConfigs[context.key]
}

fun Player.giveItems(item: String, amount: Int, map: Map<String, Any> = emptyMap()): ItemStack? {
    val item = ItemManager.getItemConfig(item) ?: return null
    val itemStack = generate(item, amount, this, map)

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