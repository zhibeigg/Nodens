package org.gitee.nodens.module.item

import kotlinx.serialization.Serializable

@Serializable
data class NormalContext(override val key: String, val variable: HashMap<String, Variable<*>>, override val hashcode: Int): IItemContext