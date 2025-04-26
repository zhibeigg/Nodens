package org.gitee.nodens.util

import kotlinx.serialization.json.Json
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.module.item.*
import taboolib.module.nms.ItemTagData
import taboolib.module.nms.getItemTag
import taboolib.platform.util.isAir

const val CONTEXT_TAG = "NODENS_CONTEXT"

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

fun Any.toVariable(): Variable<*> {
    return when (this) {
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