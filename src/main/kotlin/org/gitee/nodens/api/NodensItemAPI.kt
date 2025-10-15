package org.gitee.nodens.api

import org.bukkit.inventory.ItemStack
import org.gitee.nodens.api.interfaces.IItemAPI
import org.gitee.nodens.module.item.IItemGenerator
import org.gitee.nodens.module.item.ItemConfig
import org.gitee.nodens.module.item.ItemManager
import org.gitee.nodens.module.item.generator.NormalGenerator
import org.gitee.nodens.util.DURABILITY_TAG
import org.gitee.nodens.util.context
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.module.nms.getItemTag

class NodensItemAPI: IItemAPI {

    override fun getItemConfig(key: String): ItemConfig? {
        return ItemManager.getItemConfig(key)
    }

    override fun getItemGenerator(): IItemGenerator {
        return PlatformFactory.getAPI<IItemGenerator>()
    }

    override fun addDurability(itemStack: ItemStack, durability: Int): Int {
        val max = itemStack.context()?.get(DURABILITY_TAG) as? Int ?: 0
        val tag = itemStack.getItemTag()
        tag["durability"] = ((tag["durability"]?.asInt() ?: return 0) + durability).coerceAtMost(max)
        tag.saveTo(itemStack)
        return tag["durability"]!!.asInt()
    }

    override fun takeDurability(itemStack: ItemStack, durability: Int): Int {
        val tag = itemStack.getItemTag()
        tag["durability"] = ((tag["durability"]?.asInt() ?: return 0) - durability).coerceAtLeast(0)
        tag.saveTo(itemStack)
        return tag["durability"]!!.asInt()
    }

    override fun setDurability(itemStack: ItemStack, durability: Int): Int {
        val max = itemStack.context()?.get(DURABILITY_TAG) as? Int ?: 0
        val tag = itemStack.getItemTag()
        tag["durability"] = durability.coerceAtMost(max)
        tag.saveTo(itemStack)
        return tag["durability"]?.asInt() ?: 0
    }

    override fun getDurability(itemStack: ItemStack): Int {
        return itemStack.getItemTag()["durability"]?.asInt() ?: 0
    }

    override fun getMaxDurability(itemStack: ItemStack): Int {
        return itemStack.context()?.get(DURABILITY_TAG) as? Int ?: 0
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<IItemAPI>(NodensItemAPI())
            PlatformFactory.registerAPI<IItemGenerator>(NormalGenerator)
        }
    }
}