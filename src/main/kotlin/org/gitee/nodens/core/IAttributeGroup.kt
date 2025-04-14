package org.gitee.nodens.core

interface AttributeGroup {

    // 属性组名
    val name: String

    interface Number {

        val name: String

        val config: AttributeConfig

        fun handle()

        enum class Type {
            PERCENT, COUNT;
        }

        enum class ValueType {
            RANGE, SINGLE;
        }
    }
}