package org.gitee.nodens.module.item

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.gitee.nodens.module.item.generator.NormalGenerator
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.XEnchantment
import taboolib.library.xseries.XItemFlag
import taboolib.library.xseries.XMaterial
import kotlin.jvm.optionals.getOrElse

open class ItemConfig(open val key: String, configurationSection: ConfigurationSection) {

    // 是否开启动态更新
    open val isUpdate = configurationSection.getBoolean("update", false)

    open val material: XMaterial = XMaterial.matchXMaterial(configurationSection.getString("material", "stone")!!).getOrElse { XMaterial.STONE }
    open val name = configurationSection.getString("name", "none")!!
    open val lore = configurationSection.getStringList("lore")
    val variables = configurationSection.getConfigurationSection("variables")?.getKeys(false)?.map {
        Variable(it, configurationSection.getString("variables.$it")!!)
    } ?: emptyList()

    open val skullOwner = configurationSection.getString("skullOwner")
    open val skullTexture = configurationSection.getString("skullTexture")

    open val enchantments = configurationSection.getStringList("enchantments").associate {
        val s = it.split(":")
        if (s.size == 1) {
            XEnchantment.of(s[0]).get() to null
        } else {
            XEnchantment.of(s[0]).get() to s[1]
        }
    }
    open val itemFlags = configurationSection.getStringList("itemFlags").map { XItemFlag.of(it).get() }

    open val durability = configurationSection.getString("durability")
    open val isUnBreakable = configurationSection.getBoolean("unbreakable")

    open val sell = configurationSection.getString("sell")

    open val armourers = configurationSection.getStringList("armourers")

    // 标注版本
    open val hashCode = configurationSection.toString().hashCode()

    class Variable(val key: String, val action: String)

    fun generate(amount: Int, player: Player? = null, map: Map<String, Any> = emptyMap()): ItemStack {
        return NormalGenerator.generate(this, amount, player, map)
    }
}