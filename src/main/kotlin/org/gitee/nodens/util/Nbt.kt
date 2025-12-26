package org.gitee.nodens.util

import taboolib.common.platform.function.warning
import taboolib.common5.cbyte
import taboolib.common5.cfloat
import taboolib.common5.cshort
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.nms.ItemTagData
import taboolib.module.nms.ItemTagType
import taboolib.module.nms.ItemTagType.BYTE
import taboolib.module.nms.ItemTagType.BYTE_ARRAY
import taboolib.module.nms.ItemTagType.COMPOUND
import taboolib.module.nms.ItemTagType.DOUBLE
import taboolib.module.nms.ItemTagType.FLOAT
import taboolib.module.nms.ItemTagType.INT
import taboolib.module.nms.ItemTagType.INT_ARRAY
import taboolib.module.nms.ItemTagType.LONG
import taboolib.module.nms.ItemTagType.LONG_ARRAY
import taboolib.module.nms.ItemTagType.SHORT
import taboolib.module.nms.ItemTagType.STRING

/**
 * ```
 * nbt:
 *   test:
 *     value: '测试'
 *     type: string
 *   type:
 *     value: 1
 *     type: int
 *   武器:
 *     type: compound
 *     笔修:
 *       value:
 *         - '1'
 *         - '2'
 *         - '3'
 *       type: int_array
 *     剑修:
 *       value: 1
 *       type: int
 * ```
 * */
fun nbtParse(config: ConfigurationSection, prefix: String = ""): Map<String, ItemTagData> {
    val map = mutableMapOf<String, ItemTagData>()

    config.getKeys(false).forEach { key ->
        if (key == "nt") return@forEach
        val configSection = config.getConfigurationSection(key) ?: return@forEach

        val typeStr = configSection.getString("nt")
        if (typeStr.isNullOrBlank()) {
            warning("NBT 配置 '$prefix$key' 缺少 'nt' 类型字段")
            return@forEach
        }

        val type = try {
            ItemTagType.parse(typeStr.uppercase())
        } catch (_: Exception) {
            warning("NBT 配置 '$prefix$key' 类型无效: $typeStr")
            return@forEach
        }

        when (type) {
            COMPOUND -> map += nbtParse(configSection, "$prefix$key.")
            BYTE -> map += "$prefix$key" to ItemTagData(configSection["value"].cbyte)
            SHORT -> map += "$prefix$key" to ItemTagData(configSection["value"].cshort)
            INT -> map += "$prefix$key" to ItemTagData(configSection.getInt("value"))
            LONG -> map += "$prefix$key" to ItemTagData(configSection.getLong("value"))
            FLOAT -> map += "$prefix$key" to ItemTagData(configSection["value"].cfloat)
            DOUBLE -> map += "$prefix$key" to ItemTagData(configSection.getDouble("value"))
            STRING -> {
                val strValue = configSection.getString("value")
                if (strValue == null) {
                    warning("NBT 配置 '$prefix$key' 缺少 'value' 字段")
                    return@forEach
                }
                map += "$prefix$key" to ItemTagData(strValue)
            }
            BYTE_ARRAY -> map += "$prefix$key" to ItemTagData(configSection.getByteList("value").toByteArray())
            INT_ARRAY -> map += "$prefix$key" to ItemTagData(configSection.getIntegerList("value").toIntArray())
            LONG_ARRAY -> map += "$prefix$key" to ItemTagData(configSection.getLongList("value").toLongArray())
            else -> warning("不支持的 NBT 类型: $type (配置: $prefix$key)")
        }
    }

    return map
}