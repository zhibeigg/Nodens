package org.gitee.nodens.module.item

import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.XEnchantment
import taboolib.library.xseries.XItemFlag
import taboolib.library.xseries.XMaterial
import kotlin.jvm.optionals.getOrElse

class CloneItemConfig(override val key: String, val copy: ItemConfig, configurationSection: ConfigurationSection): ItemConfig(key, configurationSection) {

    override val isUpdate: Boolean
        get() = super.isUpdate

    override val material: XMaterial = if (configurationSection.contains("material")) {
        XMaterial.matchXMaterial(configurationSection.getString("material", "stone")!!).getOrElse { XMaterial.STONE }
    } else {
        copy.material
    }

    override val name: String = if (configurationSection.contains("name")) {
        configurationSection.getString("name", "none")!!
    } else {
        copy.name
    }
    override val lore: List<String> = if (configurationSection.contains("lore")) {
        configurationSection.getStringList("lore")
    } else {
        copy.lore
    }

    override val skullOwner = if (configurationSection.contains("skullOwner")) {
        configurationSection.getString("skullOwner")
    } else {
        copy.skullOwner
    }
    override val skullTexture = if (configurationSection.contains("skullTexture")) {
        configurationSection.getString("skullTexture")
    } else {
        copy.skullTexture
    }

    override val enchantments = if (configurationSection.contains("enchantments")) {
        configurationSection.getStringList("enchantments").associate {
            val s = it.split(":")
            if (s.size == 1) {
                XEnchantment.of(s[0]).get() to null
            } else {
                XEnchantment.of(s[0]).get() to s[1]
            }
        }
    } else {
        copy.enchantments
    }
    override val itemFlags = if (configurationSection.contains("itemFlags")) {
        configurationSection.getStringList("itemFlags").map { XItemFlag.of(it).get() }
    } else {
        copy.itemFlags
    }

    override val isUnBreakable = if (configurationSection.contains("unbreakable")) {
        configurationSection.getBoolean("unbreakable")
    } else {
        copy.isUnBreakable
    }

    override val sell = if (configurationSection.contains("sell")) {
        configurationSection.getString("sell")
    } else {
        copy.sell
    }

    override val armourers = if (configurationSection.contains("armourers")) {
        configurationSection.getStringList("armourers")
    } else {
        copy.armourers
    }

    override val hashCode = copy.hashCode + configurationSection.toString().hashCode()
}