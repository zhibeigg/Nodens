package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup

object SuckBlood: IAttributeGroup {

    override val name: String = "SuckBlood"

    object Addon: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(SuckBlood.name, name)

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            damageProcessor.onDamage(config.handlePriority) {
            }
        }
    }

}