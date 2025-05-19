package org.gitee.nodens.core.attribute

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.AttributeManager.ATTRIBUTE_MATCHING_MAP
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.debug
import org.gitee.nodens.util.files
import taboolib.common.platform.function.info
import taboolib.common5.cdouble
import taboolib.common5.scriptEngine
import taboolib.module.chat.colored
import java.io.FileReader
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.Invocable
import javax.script.SimpleBindings

object JavaScript: IAttributeGroup {

    override val name: String = "JavaScript"

    override val numbers = hashMapOf<String, JsAttribute>()

    private fun createBindings(config: AttributeConfig) = SimpleBindings(mapOf(
        "keys" to config.keys,
        "valueType" to config.valueType.name,
        "combatPower" to config.combatPower,
        "priority" to config.syncPriority
    ))

    class JsAttribute(override val name: String, val compile: CompiledScript): IAttributeGroup.Number {

        override val group: IAttributeGroup
            get() = JavaScript

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(JavaScript.name, name)

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            (compile.engine as Invocable).invokeFunction("sync", entitySyncProfile, valueMap)
        }

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            (compile.engine as Invocable).invokeFunction("handleAttacker", damageProcessor, valueMap)
        }

        override fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            (compile.engine as Invocable).invokeFunction("handleDefender", damageProcessor, valueMap)
        }

        override fun handleHealer(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            (compile.engine as Invocable).invokeFunction("handleHealer", regainProcessor, valueMap)
        }

        override fun handlePassive(regainProcessor: RegainProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
            (compile.engine as Invocable).invokeFunction("handlePassive", regainProcessor, valueMap)
        }

        override fun combatPower(valueMap: Map<DigitalParser.Type, DoubleArray>): Double {
            return (compile.engine as Invocable).invokeFunction("combatPower", valueMap).cdouble
        }
    }

    internal fun reload() {
        numbers.clear()
        files("js", "Fire.js") { file ->
            try {
                FileReader(file).use { reader ->
                    val compile = (scriptEngine as? Compilable)?.compile(reader) ?: run {
                        println("Failed to compile script: ${file.name}")
                        return@files
                    }

                    val attributeName = file.nameWithoutExtension
                    val attribute = JsAttribute(attributeName, compile)
                    numbers[attribute.name] = attribute
                    compile.eval(createBindings(attribute.config))

                    attribute.config.keys.forEach { key ->
                        ATTRIBUTE_MATCHING_MAP.put(key, attribute)
                        debug("&e┣&7AttributeKey $key loaded &a√ &7- &cJs".colored())
                    }
                }
            } catch (e: Exception) {
                System.err.println("Error loading attribute ${file.name}:")
                e.printStackTrace()
            }
        }
    }
}