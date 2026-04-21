package org.gitee.nodens.module.item.action

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.gitee.nodens.util.consoleMessage
import org.gitee.nodens.util.debug
import org.gitee.nodens.util.getConfig
import org.gitee.nodens.util.nodensEnvironmentNamespaces
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.registerBukkitListener
import taboolib.library.reflex.ReflexClass
import taboolib.module.kether.KetherShell
import taboolib.module.kether.ScriptOptions
import taboolib.module.kether.printKetherErrorMessage
import java.util.concurrent.ConcurrentHashMap

/**
 * 物品动作触发器管理器
 *
 * 职责：
 * 1. 自动扫描并注册所有 [ActionTrigger] 子类（通过 TabooLib ClassVisitor）
 * 2. 提供手动注册/注销 API（供外部插件扩展）
 * 3. 事件触发时分发到匹配的物品动作执行 Kether 脚本
 */
@Awake
object ActionTriggerManager : ClassVisitor(1) {

    /** 已注册的触发器 id -> trigger */
    private val triggers = ConcurrentHashMap<String, ActionTrigger<*>>()

    /** 待注册的触发器实例（ClassVisitor 阶段收集，ENABLE 阶段统一注册监听） */
    private val pendingTriggers = mutableListOf<ActionTrigger<*>>()

    override fun getLifeCycle(): LifeCycle = LifeCycle.LOAD

    override fun visitStart(clazz: ReflexClass) {
        val javaClass = clazz.toClass()
        if (!ActionTrigger::class.java.isAssignableFrom(javaClass)) return
        if (javaClass == ActionTrigger::class.java) return
        // 检查 @PluginDepend 注解，若依赖插件未加载则跳过
        if (clazz.hasAnnotation(PluginDepend::class.java)) {
            val pluginName = clazz.getAnnotation(PluginDepend::class.java).property<String>("name") ?: return
            val loaded = Bukkit.getPluginManager().getPlugin(pluginName) != null
            if (!loaded) {
                debug("&7触发器 ${javaClass.simpleName} 依赖插件 $pluginName 未加载，跳过")
                return
            }
        }
        val instance = clazz.getInstance() as? ActionTrigger<*> ?: return
        pendingTriggers += instance
    }

    @Awake(LifeCycle.ENABLE)
    private fun enable() {
        pendingTriggers.forEach { register(it) }
        pendingTriggers.clear()
        consoleMessage("&e┣&7ActionTrigger loaded &a${triggers.size} &7triggers &a√")
    }

    // ======================== 公开 API ========================

    /**
     * 注册一个触发器并自动监听对应事件
     * @return true 注册成功，false 已存在同 id 触发器
     */
    fun register(trigger: ActionTrigger<*>): Boolean {
        if (triggers.putIfAbsent(trigger.id, trigger) != null) {
            debug("&c触发器 ${trigger.id} 已存在，跳过注册")
            return false
        }
        registerListener(trigger)
        debug("&a注册触发器: ${trigger.id}")
        return true
    }

    /**
     * 注销一个触发器
     * 注意：已注册的 Bukkit 监听器无法动态移除，但触发器从 map 中移除后不会再执行动作
     */
    fun unregister(id: String): ActionTrigger<*>? {
        return triggers.remove(id)
    }

    /** 获取已注册的触发器 */
    fun getTrigger(id: String): ActionTrigger<*>? = triggers[id]

    /** 获取所有已注册的触发器 ID */
    fun getTriggerIds(): Set<String> = triggers.keys.toSet()

    // ======================== 内部逻辑 ========================

    @Suppress("UNCHECKED_CAST")
    private fun <E : Event> registerListener(trigger: ActionTrigger<E>) {
        registerBukkitListener(trigger.eventClass, trigger.priority, trigger.ignoreCancelled) { event ->
            // 触发器可能已被注销
            if (!triggers.containsKey(trigger.id)) return@registerBukkitListener
            val player = trigger.extract(event) ?: return@registerBukkitListener
            dispatchActions(player, trigger, event)
        }
    }

    /**
     * 分发物品动作
     * 遍历玩家装备栏中所有 Nodens 物品，匹配 actions 中的触发器并执行 Kether 脚本
     */
    private fun <E : Event> dispatchActions(player: Player, trigger: ActionTrigger<E>, event: E) {
        // 收集玩家装备栏中所有物品
        val equipment = player.equipment ?: return
        val items = buildList {
            add(equipment.itemInMainHand)
            add(equipment.itemInOffHand)
            equipment.helmet?.let { add(it) }
            equipment.chestplate?.let { add(it) }
            equipment.leggings?.let { add(it) }
            equipment.boots?.let { add(it) }
        }

        for (itemStack in items) {
            val config = itemStack.getConfig() ?: continue
            val actions = config.actions
            if (actions.isEmpty()) continue

            // 筛选匹配当前触发器的动作
            val matched = actions.filter { it.triggers.contains(trigger.id) }
            if (matched.isEmpty()) continue

            // 构建上下文
            val context = ActionContext(player, event, trigger.id)
            trigger.populate(context, event)

            // 执行 Kether 脚本
            for (action in matched) {
                try {
                    KetherShell.eval(
                        action.scripts,
                        ScriptOptions.builder()
                            .namespace(namespace = nodensEnvironmentNamespaces)
                            .sender(sender = adaptPlayer(player))
                            .set("actionContext", context)
                            .set("@Event", event)
                            .context {
                                context.variables.forEach { (key, value) ->
                                    rootFrame().variables()[key] = value
                                }
                            }
                            .build()
                    )
                } catch (e: Throwable) {
                    e.printKetherErrorMessage()
                }
            }
        }
    }
}
