package org.gitee.nodens.core.kether

import org.gitee.nodens.common.Source
import taboolib.common.OpenResult
import taboolib.common5.cdouble
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptProperty

object SourceActions {

    @KetherProperty(bind = Source::class)
    fun propertySource() = object : ScriptProperty<Source>("Source.operator") {

        override fun read(instance: Source, key: String): OpenResult {
            return when (key) {
                "key" -> OpenResult.successful(instance.key)
                "attribute" -> OpenResult.successful(instance.attribute.name)
                "amount" -> OpenResult.successful(instance.amount)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: Source, key: String, value: Any?): OpenResult {
            return when (key) {
                "amount" -> {
                    instance.amount = value.cdouble
                    OpenResult.successful()
                }
                else -> OpenResult.failed()
            }
        }
    }
}