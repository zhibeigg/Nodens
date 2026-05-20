package org.gitee.nodens.common

import org.gitee.nodens.core.IAttributeGroup

interface Source {

    val key: String

    val attribute: IAttributeGroup.Number

    val attributeGroup: String
        get() = attribute.group.name

    val attributeName: String
        get() = attribute.name

    val attributeFullName: String
        get() = "$attributeGroup:$attributeName"

    var amount: Double
}
