package org.gitee.nodens.common

import org.gitee.nodens.core.IAttributeGroup

interface Source {

    val key: String

    val attribute: IAttributeGroup.Number

    var amount: Double
}