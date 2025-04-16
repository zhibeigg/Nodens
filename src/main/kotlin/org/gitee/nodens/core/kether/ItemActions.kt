package org.gitee.nodens.core.kether

import org.gitee.nodens.module.item.ItemConfig
import taboolib.common.OpenResult
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptProperty

object ItemActions {

    @KetherProperty(bind = ItemConfig::class)
    fun propertyItemConfig() = object : ScriptProperty<ItemConfig>("ItemConfig.operator") {

        override fun read(instance: ItemConfig, key: String): OpenResult {
            return when (key) {
                "key" -> OpenResult.successful(instance.key)
                "sell" -> OpenResult.successful(instance.sell)
                "skillOwner" -> OpenResult.successful(instance.skullOwner)
                "skillTexture" -> OpenResult.successful(instance.skullTexture)
                "unbreakable" -> OpenResult.successful(instance.isUnBreakable)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: ItemConfig, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }
}