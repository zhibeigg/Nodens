package org.gitee.nodens.core.entity

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause.*
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.*
import org.gitee.nodens.api.events.player.NodensPlayerExpChangeEvents
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.core.attribute.Damage
import org.gitee.nodens.core.attribute.Exp
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.platform.util.attacker

object EntityListener {

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun onSwapHandItems(e: PlayerSwapHandItemsEvent) {
        if (e.isCancelled) return
        e.player.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun onInventoryClick(e: InventoryClickEvent) {
        if (e.isCancelled) return
        e.whoClicked.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun onInventoryDrag(e: InventoryDragEvent) {
        if (e.isCancelled) return
        e.whoClicked.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun onItemHeld(e: PlayerItemHeldEvent) {
        if (e.isCancelled) return
        e.player.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun onDropItem(e: PlayerDropItemEvent) {
        if (e.isCancelled) return
        e.player.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun onItemBreak(e: PlayerItemBreakEvent) {
        e.player.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun onInteract(e: PlayerInteractEvent) {
        if (e.useItemInHand() == Event.Result.DENY) return
        e.player.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent
    private fun regain(e: EntityRegainHealthEvent) {
        if (e.regainReason != EntityRegainHealthEvent.RegainReason.CUSTOM) {
            e.isCancelled = true
        }
    }

    /**
     * 攻击检测
     * */
    @SubscribeEvent(EventPriority.HIGHEST)
    private fun attack(e: EntityDamageByEntityEvent) {
        if (e.isCancelled) return
        if (e.isApplicable(EntityDamageEvent.DamageModifier.ARMOR)) {
            e.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0.0)
        }
        if (e.isApplicable(EntityDamageEvent.DamageModifier.MAGIC)) {
            e.setDamage(EntityDamageEvent.DamageModifier.MAGIC, 0.0)
        }

        val processor: DamageProcessor = when (e.cause) {
            ENTITY_ATTACK, ENTITY_SWEEP_ATTACK  -> {
                DamageProcessor(Damage.Physics.name, e.attacker ?: return, e.entity as? LivingEntity ?: return)
            }
            MAGIC, DRAGON_BREATH, PROJECTILE, ENTITY_EXPLOSION -> {
                DamageProcessor(Damage.Magic.name, e.attacker ?: return, e.entity as? LivingEntity ?: return)
            }
            else -> return
        }
        processor.handle()
        if (e.entity is Player) {
            e.setDamage(EntityDamageEvent.DamageModifier.BLOCKING, 0.0)
            runCatching { e.setDamage(EntityDamageEvent.DamageModifier.HARD_HAT, 0.0) }
        }
        e.setDamage(EntityDamageEvent.DamageModifier.BASE, processor.getFinalDamage())
        submit {
            if (!e.isCancelled) {
                processor.callback(e.finalDamage)
            }
        }
    }

    /**
     * 经验加成
     * */
    @SubscribeEvent(EventPriority.LOWEST)
    private fun exp(e: PlayerExpChangeEvent) {
        val addon = e.player.attributeMemory()?.mergedAttribute(Exp.Addon) ?: return
        val addonValue = ((addon[PERCENT]?.get(0) ?: 0.0) * e.amount.cdouble).cint
        val event = NodensPlayerExpChangeEvents.Pre(e.player, e.amount, addonValue)
        if (event.call()) {
            e.amount += event.addon
            NodensPlayerExpChangeEvents.Post(e.player, event.amount, event.addon).call()
        }
    }
}