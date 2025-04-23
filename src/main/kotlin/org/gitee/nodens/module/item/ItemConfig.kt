package org.gitee.nodens.module.item

import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.XEnchantment
import taboolib.library.xseries.XItemFlag
import taboolib.library.xseries.XMaterial
import kotlin.jvm.optionals.getOrElse

class ItemConfig(val key: String, configurationSection: ConfigurationSection) {

    // 是否开启动态更新
    val isUpdate = configurationSection.getBoolean("update", false)

    val material = XMaterial.matchXMaterial(configurationSection.getString("material", "stone")!!).getOrElse { XMaterial.STONE }
    val name = configurationSection.getString("name", "none")!!
    val lore = configurationSection.getStringList("lore")
    val variables = configurationSection.getConfigurationSection("variables")?.getKeys(false)?.map {
        Variable(it, configurationSection.getString("variables.$it")!!)
    } ?: emptyList()

    val skullOwner = configurationSection.getString("skullOwner")
    val skullTexture = configurationSection.getString("skullTexture")

    val enchantments = configurationSection.getStringList("enchantments").associate {
        val s = it.split(":")
        if (s.size == 1) {
            XEnchantment.of(s[0]).get() to null
        } else {
            XEnchantment.of(s[0]).get() to s[1]
        }
    }
    val itemFlags = configurationSection.getStringList("itemFlags").map { XItemFlag.of(it).get() }

    val isUnBreakable = configurationSection.getBoolean("unbreakable")

    val sell = configurationSection.getString("sell")

    // 标注版本
    val hashCode = configurationSection.toString().hashCode()

    class Variable(val key: String, val action: String)
}