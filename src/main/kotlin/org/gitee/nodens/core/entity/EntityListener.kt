package org.gitee.nodens.core.entity

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause.*
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.*
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.core.attribute.Damage
import org.gitee.nodens.core.attribute.Exp
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.platform.util.attacker

object EntityListener {

    @SubscribeEvent
    private fun onSwapHandItems(e: PlayerSwapHandItemsEvent) {
        e.player.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent
    private fun onInventoryClick(e: InventoryClickEvent) {
        e.whoClicked.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent
    private fun onInventoryDrag(e: InventoryDragEvent) {
        e.whoClicked.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent
    private fun onItemHeld(e: PlayerItemHeldEvent) {
        e.player.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent
    private fun onDropItem(e: PlayerDropItemEvent) {
        e.player.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent
    private fun onItemBreak(e: PlayerItemBreakEvent) {
        e.player.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent
    private fun onInteract(e: PlayerInteractEvent) {
        e.player.attributeMemory()?.updateAttributeAsync()
    }

    @SubscribeEvent
    private fun regain(e: EntityRegainHealthEvent) {
        if (e.regainReason == EntityRegainHealthEvent.RegainReason.EATING) {
            e.isCancelled = true
        }
    }

    /**
     * 攻击检测
     * */
    @SubscribeEvent(EventPriority.LOWEST)
    private fun attack(e: EntityDamageByEntityEvent) {
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
        e.setDamage(EntityDamageEvent.DamageModifier.ABSORPTION, 0.0)
        e.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0.0)
        e.setDamage(EntityDamageEvent.DamageModifier.BLOCKING, 0.0)
        e.setDamage(EntityDamageEvent.DamageModifier.HARD_HAT, 0.0)
        e.setDamage(EntityDamageEvent.DamageModifier.MAGIC, 0.0)
        e.setDamage(EntityDamageEvent.DamageModifier.RESISTANCE, 0.0)
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
        val double = 1.0 + (addon[PERCENT]?.get(0) ?: 0.0)
        e.amount = (double * e.amount.cdouble).cint
    }

}