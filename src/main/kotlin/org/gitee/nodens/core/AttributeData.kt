package org.gitee.nodens.core

import org.gitee.nodens.common.DigitalParser

class AttributeData(override val attributeNumber: IAttributeGroup.Number, override val value: DigitalParser.Value): IAttributeData {

    override fun toString(): String {
        return "attributeNumber=${attributeNumber.group.name}:${attributeNumber.name}, type=${value.type}, value=${value.doubleArray}"
    }
}