package org.gitee.nodens.module.item

import org.gitee.nodens.util.nbtParse
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.XEnchantment
import taboolib.library.xseries.XItemFlag
import taboolib.library.xseries.XMaterial
import kotlin.jvm.optionals.getOrElse

class CloneItemConfig(override val key: String, val copy: ItemConfig, private val configurationSection: ConfigurationSection): ItemConfig(key, configurationSection) {

    override val isUpdate: Boolean = if (configurationSection.contains("update")) {
        configurationSection.getBoolean("update", false)
    } else {
        copy.isUpdate
    }

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

    override val durability = if (configurationSection.contains("durability")) {
        configurationSection.getString("durability")
    } else {
        copy.durability
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

    override val nbt = if (configurationSection.contains("nbt")) {
        configurationSection.getConfigurationSection("nbt")?.let { nbtParse(it) }
    } else {
        copy.nbt
    }

    override fun get(path: String): Any? {
        return if (configurationSection.contains(path)) {
            configurationSection[path]
        } else {
            copy[path]
        }
    }

    override fun get(path: String, def: Any?): Any? {
        return if (configurationSection.contains(path)) {
            configurationSection[path, def]
        } else {
            copy[path, def]
        }
    }

    override val hashCode = copy.hashCode + configurationSection.toString().hashCode()
}