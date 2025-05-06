package org.gitee.nodens.module.item

import kotlinx.serialization.Serializable

@Serializable
sealed interface Variable<T> {
    
    val value: T?
}