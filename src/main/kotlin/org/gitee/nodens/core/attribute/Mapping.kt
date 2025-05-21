package org.gitee.nodens.core.attribute

import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.util.ReloadableLazy
import org.gitee.nodens.util.mergeValues
import org.gitee.nodens.util.nodensEnvironmentNamespaces
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.KetherFunction
import taboolib.module.kether.ScriptOptions
import taboolib.module.kether.runKether

object Mapping: IAttributeGroup {

    override val name: String = "Mapping"

    override val numbers = hashMapOf<String, MappingAttribute>()

    class MappingAttribute(override val name: String) : AbstractNumber() {

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(group.name, name)

        override val group: IAttributeGroup
            get() = Mapping

        private val attributes by ReloadableLazy({ config }) { config.getStringList("attributes") }

        fun getAttributes(entity: LivingEntity, map: Map<DigitalParser.Type, DoubleArray>): Map<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>> {
            val count = map[DigitalParser.Type.COUNT]?.get(0) ?: 0.0
            val percent = map[DigitalParser.Type.PERCENT]?.get(0) ?: 0.0
            val value = count * (1 + percent)
            val hashMap = hashMapOf<IAttributeGroup.Number, Map<DigitalParser.Type, DoubleArray>>()

            val list = runKether {
                KetherFunction.parse(
                    attributes,
                    ScriptOptions.builder().sender(adaptPlayer(entity)).set("value", value).namespace(nodensEnvironmentNamespaces).build()
                )
            }?.mapNotNull {
                AttributeManager.ATTRIBUTE_MATCHING_MAP.getMatchResult(it)
            } ?: return hashMap

            list.groupBy { it.value }.forEach { (key, list) ->
                val values = list.mapNotNull { DigitalParser(it.remain ?: return@mapNotNull null, key).getValue() }
                hashMap[key] = mergeValues(*values.toTypedArray())
            }
            return hashMap
        }

        override fun toString(): String {
            return "MappingAttributeNumber{name: ${name}}"
        }
    }
}