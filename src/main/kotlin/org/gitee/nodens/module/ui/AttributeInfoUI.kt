package org.gitee.nodens.module.ui

import eos.moe.dragoncore.api.gui.event.CustomPacketEvent
import eos.moe.dragoncore.network.PacketSender
import org.bukkit.Bukkit
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.util.comparePriority
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common5.format

class AttributeInfoUI {

    companion object {

        @Ghost
        @SubscribeEvent
        private fun packet(e: CustomPacketEvent) {
            if (e.identifier == "NodensAttributeInfo") {
                val player = Bukkit.getPlayerExact(e.data[0]) ?: return
                val source = player.attributeMemory()?.mergedAllAttribute(true)?.toSortedMap { o1, o2 ->
                    val priorityCompare = comparePriority(o1.config.handlePriority, o2.config.handlePriority)
                    // 当优先级相同时，用名称区分，避免 TreeMap 覆盖
                    if (priorityCompare != 0) priorityCompare else o1.name.compareTo(o2.name)
                }
                val map = source?.map {
                    it.key to it.key.getFinalValue(player, it.value)
                }?.associate {
                    when(it.first.config.valueType) {
                        IAttributeGroup.Number.ValueType.SINGLE -> "${it.first.group.name}:${it.first.name}" to it.second.value!!.toString()
                        IAttributeGroup.Number.ValueType.RANGE -> "${it.first.group.name}:${it.first.name}" to "${it.second.rangeValue!!.left} - ${it.second.rangeValue!!.right}"
                    }
                } ?: return
                val explicitMap = source.map { (key, value) ->
                    "${key.group.name}:${key.name}:explicit" to "${value[DigitalParser.Type.COUNT]?.joinToString("-") { it.format(1).toString() } ?: 0} + ${value[DigitalParser.Type.PERCENT]?.joinToString("-") { (it * 100).format(1).toString() } ?: 0}%"
                }
                PacketSender.sendSyncPlaceholder(e.player, map + explicitMap)
            }
        }
    }
}