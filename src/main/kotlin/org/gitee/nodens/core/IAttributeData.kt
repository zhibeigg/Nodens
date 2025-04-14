package org.gitee.nodens.core

import org.gitee.nodens.common.DigitalParser

interface IAttributeData {

    val attributeNumber: IAttributeGroup.Number

    val value: DigitalParser.Value
}