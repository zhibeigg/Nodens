package org.gitee.nodens.core.attribute

import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup

object Exp: IAttributeGroup {

    override val name: String = "Exp"

    object Addon: AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(Exp.name, name)
    }
}