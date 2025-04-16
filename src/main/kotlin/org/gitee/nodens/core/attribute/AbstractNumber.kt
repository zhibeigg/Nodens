package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.IAttributeGroup

abstract class AbstractNumber: IAttributeGroup.Number {

    override val name: String
        get() = this::class.java.simpleName

    override fun sync(
        entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    }

    override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    }

    override fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
    }
}