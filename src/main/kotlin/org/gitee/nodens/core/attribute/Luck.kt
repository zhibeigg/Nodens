package org.gitee.nodens.core.attribute

import org.gitee.nodens.core.IAttributeGroup

object Luck: IAttributeGroup {

    override val name: String = "Luck"

    override val numbers: Map<String, IAttributeGroup.Number> = arrayOf(Max).associateBy { it.name }

    object Max: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Luck
    }
}