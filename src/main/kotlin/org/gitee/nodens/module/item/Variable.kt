package org.gitee.nodens.module.item

import java.io.Serializable

interface Variable<T: Serializable>{
    
    val value: T?
}