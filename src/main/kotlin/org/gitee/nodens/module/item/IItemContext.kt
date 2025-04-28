package org.gitee.nodens.module.item

import kotlinx.serialization.Serializable

@Serializable
sealed interface IItemContext {

    val key: String

    val hashcode: Int
}