package org.gitee.nodens.common

import com.eatthepath.uuid.FastUUID
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.gitee.nodens.core.reload.Reload
import org.gitee.nodens.util.getBytes
import org.gitee.nodens.util.maxHealth
import org.gitee.nodens.util.nodensEnvironmentNamespaces
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.info
import taboolib.common5.cdouble
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.kether.*
import taboolib.module.nms.MinecraftVersion
import java.util.*

object Handle {

    @Config("handle.yml")
    lateinit var handle: ConfigFile
        private set

    private const val SCRIPT_ON_DAMAGE = "nodens_onDamage"
    private const val SCRIPT_ON_REGAIN = "nodens_onRegain"

    private val ketherScriptLoader by lazy { KetherScriptLoader() }
    private val catchMap by lazy { hashMapOf<String, Script>() }

    val onDamage: Script
        get() = catchMap[SCRIPT_ON_DAMAGE]!!

    val onRegain: Script
        get() = catchMap[SCRIPT_ON_REGAIN]!!

    private fun getScript(id: String, action: String): Script? {
        return try {
            ketherScriptLoader.load(ScriptService, id, getBytes(action), nodensEnvironmentNamespaces)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun init() {
        catchMap.clear()
        catchMap[SCRIPT_ON_DAMAGE] = getScript(SCRIPT_ON_DAMAGE, handle.getString("onDamage")!!) ?: error("请补充handle.yml中的onDamage脚本")
        catchMap[SCRIPT_ON_REGAIN] = getScript(SCRIPT_ON_REGAIN, handle.getString("onRegain")!!) ?: error("请补充handle.yml中的onRegain脚本")
        info("&e┣&7Handle loaded &a√".colored())
    }

    fun runProcessor(damageProcessor: DamageProcessor): Double {
        return ScriptContext.create(onDamage) {
            sender = adaptCommandSender(damageProcessor.attacker)
            id = FastUUID.toString(UUID.randomUUID())
            this["damageType"] = damageProcessor.damageType
            this["damageSources"] = damageProcessor.damageSources.values.toList()
            this["defenceSources"] = damageProcessor.defenceSources.values.toList()
            this["attacker"] = damageProcessor.attacker
            this["defender"] = damageProcessor.defender
            this["scale"] = damageProcessor.scale
        }.runActions().orNull().cdouble
    }

    fun runProcessor(regainProcessor: RegainProcessor): Double {
        return ScriptContext.create(onRegain) {
            sender = adaptCommandSender(regainProcessor.healer)
            id = FastUUID.toString(UUID.randomUUID())
            this["reason"] = regainProcessor.reason
            this["regainSources"] = regainProcessor.regainSources.values.toList()
            this["reduceSources"] = regainProcessor.reduceSources.values.toList()
            this["healer"] = regainProcessor.healer
            this["passive"] = regainProcessor.passive
            this["scale"] = regainProcessor.scale
        }.runActions().orNull().cdouble
    }

    fun doDamage(attacker: LivingEntity?, defender: LivingEntity, damageCause: DamageCause, damage: Double): EntityDamageByEntityEvent? {
        if (defender.noDamageTicks != 0) defender.noDamageTicks = 0
        // 如果实体血量 - 预计伤害值 < 0 提前设置击杀者
        if (defender.health - damage <= 0 && attacker is Player) defender.setKiller(attacker)
        val event = if (attacker != null) EntityDamageByEntityEvent(attacker, defender, damageCause, damage).also { defender.lastDamageCause = it } else null
        defender.damage(damage)
        return event
    }

    fun doHeal(passive: LivingEntity, regain: Double): EntityRegainHealthEvent? {
        val event = EntityRegainHealthEvent(passive, regain, EntityRegainHealthEvent.RegainReason.CUSTOM)
        passive.health = (passive.health + regain).coerceIn(0.0, passive.maxHealth())
        return event
    }

    private fun LivingEntity.setKiller(killer: LivingEntity) {
        when (MinecraftVersion.major) {
            // 1.12.* 1.16.*
            4, 8 -> setProperty("entity/killer", killer.getProperty("entity"))
            // 1.15.* 1.17.* bc
            7, 9 -> setProperty("entity/bc", killer.getProperty("entity"))
            // 1.18.2 bc 1.18.1 bd
            10 -> if (MinecraftVersion.minecraftVersion == "v1_18_R2") {
                setProperty("entity/bc", killer.getProperty("entity"))
            } else {
                setProperty("entity/bd", killer.getProperty("entity"))
            }
            // 1.18.* 1.19.* bd
            11 -> setProperty("entity/bd", killer.getProperty("entity"))
        }
    }
}