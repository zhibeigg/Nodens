package org.gitee.nodens.core

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlayerAttributeMemory(val player: Player) {

    companion object {

        private val playerAttributeMemoriesMap = hashMapOf<UUID, PlayerAttributeMemory>()

        @SubscribeEvent
        private fun onPlayerJoinEvent(event: PlayerJoinEvent) {
            playerAttributeMemoriesMap[event.player.uniqueId] = PlayerAttributeMemory(event.player)
        }

        @SubscribeEvent
        private fun onPlayerQuitEvent(event: PlayerQuitEvent) {
            playerAttributeMemoriesMap.remove(event.player.uniqueId)
        }

        fun Player.attributeMemory(): PlayerAttributeMemory? {
            return playerAttributeMemoriesMap[uniqueId]
        }
    }

    private val memory by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<String, TempAttributeData>() }

    fun addAttribute(name: String, value: TempAttributeData) {
        memory[name] = value
    }

    fun removeAttribute(name: String, value: TempAttributeData): TempAttributeData? {
        return memory.remove(name)
    }

    fun updateAttribute() {
        val iterator = memory.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.closed) {
                iterator.remove()
            }
        }
    }
}