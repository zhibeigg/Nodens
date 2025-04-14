package org.gitee.nodens.core

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile

interface IAttributeGroup {

    // 属性组名
    val name: String

    interface Number {

        val name: String

        val config: AttributeConfig

        fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>)

        fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>)

        fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>)

        enum class ValueType {
            RANGE, SINGLE;
        }
    }
}