package org.gitee.nodens.core.attribute

import org.gitee.nodens.core.IAttributeGroup

object Exp: IAttributeGroup {

    override val name: String = "Exp"

    override val numbers: Map<String, IAttributeGroup.Number> = arrayOf(Addon).associateBy { it.name }

    object Addon: AbstractNumber() {

        override val group: IAttributeGroup
            get() = Exp
    }
}