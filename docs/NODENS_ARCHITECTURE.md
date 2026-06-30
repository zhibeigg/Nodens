# Nodens 游戏属性插件架构文档

## 目录
- [1. 项目概述](#1-项目概述)
- [2. 插件运行逻辑](#2-插件运行逻辑)
- [3. 属性系统架构](#3-属性系统架构)
- [4. 属性处理规则](#4-属性处理规则)
- [5. 伤害计算流程](#5-伤害计算流程)
- [6. 恢复计算流程](#6-恢复计算流程)
- [7. 核心类说明](#7-核心类说明)
- [8. 公开 API 边界](#8-公开-api-边界)
- [9. MythicMobs 兼容层](#9-mythicmobs-兼容层)

---

## 1. 项目概述

Nodens 是一个基于 TabooLib 6.2 框架开发的 Minecraft RPG 属性系统插件，使用 Kotlin 2.1.20 编写。

### 技术特点
- **Caffeine 缓存**: 高性能实体属性缓存（5分钟TTL）
- **Kotlin 协程**: 异步属性更新处理
- **Kether 脚本引擎**: 支持动态伤害/恢复公式配置
- **FastMatchingMap**: 基于 Trie 树的 O(m) 时间复杂度属性匹配
- **PRD 算法**: 伪随机分布算法实现暴击机制
- **热重载系统**: 带优先级的配置重载机制

---

## 2. 插件运行逻辑

### 2.1 启动流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                        插件启动流程                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  LifeCycle.INIT │
                    │  注册 NodensAPI  │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ LifeCycle.ENABLE│
                    │  初始化各模块    │
                    └────────┬────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
    ┌─────────────────┐ ┌──────────────┐ ┌──────────────┐
    │ AttributeManager│ │    Handle    │ │ RegainTask   │
    │   加载属性配置   │ │ 加载计算脚本 │ │ 启动恢复任务 │
    └─────────────────┘ └──────────────┘ └──────────────┘
              │               │               │
              └───────────────┼───────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ LifeCycle.ACTIVE│
                    │   插件就绪      │
                    └─────────────────┘
```

### 2.2 核心模块初始化顺序

| 优先级 | 模块 | 说明 |
|--------|------|------|
| 0 | AttributeManager | 加载属性组和配置文件 |
| 1 | Handle | 加载伤害/恢复计算脚本 |
| 1 | ISyncCache | 初始化同步缓存 |
| 1 | RegainTask | 启动生命恢复定时任务 |

---

## 3. 属性系统架构

### 3.1 属性层次结构

```
┌─────────────────────────────────────────────────────────────────┐
│                      IAttributeGroup (属性组接口)                │
│  - name: String (属性组名)                                       │
│  - numbers: Map<String, Number> (属性映射)                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    IAttributeGroup.Number (属性接口)             │
│  - group: IAttributeGroup                                        │
│  - name: String                                                  │
│  - config: AttributeConfig                                       │
│  - sync(): 同步到 Bukkit                                         │
│  - handleAttacker(): 处理攻击者                                  │
│  - handleDefender(): 处理防御者                                  │
│  - handleHealer(): 处理治疗者                                    │
│  - handlePassive(): 处理被治疗者                                 │
│  - combatPower(): 计算战斗力                                     │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 属性组列表

| 属性组 | 包含属性 | 说明 |
|--------|----------|------|
| **Damage** | Physics, Magic, Real, Fire, Monster, Boss, Enhancement | 伤害类属性 |
| **Defence** | Physics, Magic, Fire, Reduction | 防御类属性 |
| **Health** | Max, Regain, RegainAddon, Heal, GrievousWounds, Healer | 生命类属性 |
| **Crit** | PhysicalChance, MagicChance, Addon, CritChanceResistance, CritAddonResistance | 暴击类属性 |
| **Speed** | Attack, Move | 速度类属性 |
| **Mana** | Max, Regain | 法力类属性 |
| **SuckBlood** | Value, Addon | 吸血类属性 |
| **Exp** | Addon | 经验类属性 |
| **Luck** | Value | 幸运类属性 |
| **Mapping** | 动态映射 | 属性键名映射 |
| **JavaScript** | 动态脚本 | JavaScript 动态属性 |

### 3.3 属性数据流

```
┌──────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  物品 Lore   │────▶│ FastMatchingMap  │────▶│  IAttributeData │
│  属性文本    │     │   Trie 树匹配    │     │    属性数据     │
└──────────────┘     └──────────────────┘     └─────────────────┘
                              │
                              ▼
                     ┌──────────────────┐
                     │  DigitalParser   │
                     │   数值解析器     │
                     │ (COUNT/PERCENT)  │
                     └──────────────────┘
```

---

## 4. 属性处理规则

### 4.1 属性匹配流程

```
输入: "§7物理攻击: 100-200"
         │
         ▼
┌─────────────────────────────────────┐
│        FastMatchingMap.preprocess   │
│  1. 移除颜色代码 (§7)               │
│  2. 移除空白字符                    │
│  3. 移除冒号                        │
│  结果: "物理攻击100-200"            │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│        Trie 树匹配                   │
│  匹配 "物理攻击" → Damage.Physics   │
│  剩余: "100-200"                    │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│        DigitalParser 解析           │
│  "100-200" → [100.0, 200.0]         │
│  类型: COUNT (数值型)               │
│  或 "10%-20%" → [0.1, 0.2]          │
│  类型: PERCENT (百分比型)           │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│        AttributeData                │
│  attributeNumber: Damage.Physics    │
│  value: {COUNT: [100.0, 200.0]}     │
└─────────────────────────────────────┘
```

### 4.2 属性值类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **SINGLE** | 单一数值 | `100` |
| **RANGE** | 范围数值 | `100-200` |
| **COUNT** | 固定数值 | `+100` |
| **PERCENT** | 百分比 | `+10%` |

### 4.3 属性配置结构

```yaml
Physics:
  combatPower: 1.0      # 战斗力系数
  valueType: RANGE      # 值类型: SINGLE/RANGE
  handlePriority: 1     # 处理优先级 (数值小优先)
  keys:                 # 识别关键字
    - 物理攻击
```

---

## 5. 伤害计算流程

### 5.1 伤害处理流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                        伤害计算流程                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ DamageProcessor │
                    │   创建处理器    │
                    │ (damageType,    │
                    │  attacker,      │
                    │  defender)      │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   handle()      │
                    │ 处理攻防双方属性│
                    └────────┬────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐
    │ handleAttacker  │             │ handleDefender  │
    │ 收集伤害源      │             │ 收集防御源      │
    │ - Physics       │             │ - Physics       │
    │ - Magic         │             │ - Magic         │
    │ - Real          │             │ - Fire          │
    │ - Fire          │             │ - Reduction     │
    │ - Monster/Boss  │             └────────┬────────┘
    │ - Enhancement   │                      │
    │ - Crit.Addon    │                      │
    └────────┬────────┘                      │
              │                               │
              └───────────────┬───────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  暴击判定       │
                    │ PhysicalChance  │
                    │ MagicChance     │
                    │ - 抗性计算      │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ getFinalDamage  │
                    │ 执行 Kether 脚本│
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  callDamage()   │
                    │ 触发伤害事件    │
                    └────────┬────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐
    │ Pre 事件        │             │ Post 事件       │
    │ (可取消)        │             │ (伤害后回调)    │
    └─────────────────┘             │ - 吸血处理      │
                                    └─────────────────┘
```

### 5.2 伤害计算公式 (handle.yml)

```
最终伤害 = ((基础伤害 + 增伤) × (1 - 防御/(1000+防御)) + 伤害提升) × 缩放系数

其中:
- 基础伤害 = 物理伤害 + 魔法伤害
- 增伤 = 小怪增伤 + Boss增伤
- 真实伤害 = (真实伤害 + 伤害提升) × 缩放系数 (无视防御)
```

### 5.3 暴击机制

```
┌─────────────────────────────────────────────────────────────────┐
│                        暴击计算流程                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 获取暴击率      │
                    │ PhysicalChance  │
                    │ 或 MagicChance  │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 获取暴击抗性    │
                    │ CritChance-     │
                    │ Resistance      │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 实际暴击率 =    │
                    │ 暴击率 - 抗性   │
                    │ (最小0, 最大1)  │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ random() 判定   │
                    └────────┬────────┘
                              │
              ┌───────────────┴───────────────┐
              │ 暴击成功                       │ 暴击失败
              ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐
    │ 添加暴击伤害    │             │ 正常伤害        │
    │ Crit.Addon      │             └─────────────────┘
    │ 暴击伤害 =      │
    │ 基础伤害 × 暴击 │
    │ 伤害加成        │
    └────────┬────────┘
              │
              ▼
    ┌─────────────────┐
    │ 暴击伤害抗性    │
    │ CritAddon-      │
    │ Resistance      │
    │ 减免暴击伤害    │
    └─────────────────┘
```

### 5.4 DamageProcessor 核心属性

| 属性 | 类型 | 说明 |
|------|------|------|
| damageType | String | 攻击类型 (PHYSICS/MAGIC/REAL) |
| attacker | LivingEntity | 攻击者 |
| defender | LivingEntity | 防御者 |
| scale | Double | 伤害缩放系数 (默认1.0) |
| crit | Boolean | 是否暴击 |
| damageSources | Map | 伤害源集合 |
| defenceSources | Map | 防御源集合 |

---

## 6. 恢复计算流程

### 6.1 恢复处理流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                        恢复计算流程                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ RegainProcessor │
                    │   创建处理器    │
                    │ (reason,        │
                    │  healer,        │
                    │  passive)       │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   handle()      │
                    │ 处理治疗双方属性│
                    └────────┬────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐
    │  handleHealer   │             │ handlePassive   │
    │ 收集恢复源      │             │ 收集减疗源      │
    │ - Heal (治疗力) │             │ - Regain        │
    │ - Healer (治疗  │             │ - RegainAddon   │
    │   能力提升)     │             │ - GrievousWounds│
    └────────┬────────┘             │   (重伤)        │
              │                     └────────┬────────┘
              │                               │
              └───────────────┬───────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ getFinalRegain  │
                    │ 执行 Kether 脚本│
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  callRegain()   │
                    │ 触发恢复事件    │
                    └────────┬────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐
    │ Pre 事件        │             │ Post 事件       │
    │ (可取消)        │             │ (恢复后回调)    │
    └─────────────────┘             └─────────────────┘
```

### 6.2 恢复计算公式 (handle.yml)

```
最终恢复 = (恢复源总和 - 减疗源总和) × 缩放系数

其中:
- 恢复源 = 生命恢复 + 百分比生命恢复 + 治疗力 + 治疗能力提升
- 减疗源 = 重伤效果
```

### 6.3 自然恢复机制

```
┌─────────────────────────────────────────────────────────────────┐
│                      自然恢复定时任务                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 每 period tick  │
                    │ (默认20 = 1秒)  │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 遍历在线玩家    │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 创建 Regain-    │
                    │ Processor       │
                    │ reason=NATURAL  │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 处理 Regain     │
                    │ 和 RegainAddon  │
                    │ 属性            │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 同步执行恢复    │
                    │ callRegain()    │
                    └─────────────────┘
```

### 6.4 RegainProcessor 核心属性

| 属性 | 类型 | 说明 |
|------|------|------|
| reason | String | 恢复原因 (NATURAL/其他) |
| healer | LivingEntity | 治疗者 |
| passive | LivingEntity | 被治疗者 |
| scale | Double | 恢复缩放系数 (默认1.0) |
| regainSources | Map | 恢复源集合 |
| reduceSources | Map | 减疗源集合 |

---

## 7. 核心类说明

### 7.1 EntityAttributeMemory

实体属性内存管理器，负责管理实体的所有属性数据。

```kotlin
class EntityAttributeMemory(val entity: LivingEntity) {
    // 临时属性存储
    private val extendMemory: ConcurrentHashMap<String, TempAttributeData>

    // 实体同步配置
    val entitySyncProfile: EntitySyncProfile

    // 核心方法
    fun addAttribute(name: String, value: TempAttributeData)  // 添加临时属性
    fun removeAttribute(name: String): TempAttributeData?     // 移除临时属性
    fun getItemsAttribute(): List<IAttributeData>             // 获取物品属性
    fun mergedAllAttribute(): Map<Number, Map<Type, DoubleArray>>  // 合并所有属性
    fun updateAttributeAsync()                                // 异步更新属性
    fun syncAttributeToBukkit()                               // 同步到 Bukkit
}
```

### 7.2 FastMatchingMap

基于 Trie 树的高性能属性匹配器。

```kotlin
class FastMatchingMap<T>(
    ignoreSpace: Boolean = true,   // 忽略空格
    ignoreColor: Boolean = true,   // 忽略颜色代码
    ignoreColon: Boolean = true,   // 忽略冒号
    ignorePrefix: Boolean = true   // 允许前缀匹配
) {
    fun put(key: String, value: T)                    // 添加匹配规则
    fun get(lore: String): T?                         // 获取匹配值
    fun getMatchResult(lore: String): MatchResult<T>? // 获取匹配结果和剩余字符串
}
```

### 7.3 Handle

伤害/恢复计算引擎，执行 Kether 脚本。

```kotlin
object Handle {
    val onDamage: Script  // 伤害计算脚本
    val onRegain: Script  // 恢复计算脚本

    fun runProcessor(damageProcessor: DamageProcessor): Double  // 执行伤害计算
    fun runProcessor(regainProcessor: RegainProcessor): Double  // 执行恢复计算
    fun doDamage(attacker, defender, cause, damage): Event?     // 执行伤害
    fun doHeal(passive, regain): Event?                         // 执行治疗
}
```

---

## 8. 公开 API 边界

Nodens 对外提供三层入口：

| 入口 | 说明 |
|------|------|
| `Nodens.api()` | 获取完整 `INodensAPI` 门面 |
| `Nodens.attributeAPI()` / `Nodens.itemAPI()` / `Nodens.reloadAPI()` | 获取分模块 API |
| `Nodens.reloadAttributes()`、`Nodens.ensureAttributeMemory()` 等静态快捷方法 | 面向常用场景的直接调用 |

### 8.1 属性组注册

运行期属性组通过 `AttributeManager` 的外部注册表接入。重载属性时会先扫描插件内置 `IAttributeGroup`，再合并运行期注册的属性组，并原子替换运行时 `groupMap` 和属性配置表。

```kotlin
Nodens.registerAttributeGroup(myGroup)
Nodens.unregisterAttributeGroup("MyGroup")
Nodens.getAttributeGroups()
Nodens.getAttributeConfig("Health", "Max")
```

`registerAttributeGroup(group, reloadAttributes = true)` 默认会重建属性匹配表，并刷新当前 `EntityAttributeMemory`，使新属性尽快对在线实体生效。

### 8.2 实体属性内存

`EntityAttributeMemory` 现在同时支持只读获取和确保创建：

```kotlin
Nodens.getEntityAttributeMemory(entity) // 不存在则返回 null
Nodens.ensureAttributeMemory(entity)    // 不存在则创建
Nodens.removeAttributeMemory(entity)
Nodens.updateAllAttributes()
```

这使外部插件可以为非玩家实体、召唤物或脚本生成实体主动建立属性内存。

### 8.3 分模块重载

`IReloadAPI` 支持完整重载、按权重重载和常用模块重载：

```kotlin
Nodens.reload()
Nodens.reloadConfig()
Nodens.reloadHandle()
Nodens.reloadAttributes(updateEntities = true)
Nodens.reloadItems()
Nodens.reloadItemGroups()
Nodens.reloadConditions()
Nodens.reloadFormulas()
Nodens.reloadRegainTask()
Nodens.reloadByWeight(0)
```

完整重载仍会触发 `NodensPluginReloadEvent`，外部插件可继续通过事件注册带权重的扩展重载函数。

### 8.4 纯内存属性配置

外部插件可以通过 `AttributeRegistrationConfig` 注册运行期属性组，不再需要构造 `ConfigurationSection` 或写入临时配置文件：

```kotlin
Nodens.registerAttributeGroup(
    myGroup,
    mapOf(
        "Power" to AttributeRegistrationConfig(
            keys = listOf("力量"),
            valueType = IAttributeGroup.Number.ValueType.SINGLE,
            combatPower = 1.0,
            syncPriority = 0,
            handlePriority = 1,
        )
    )
)
```

该配置会进入运行期属性配置表，并参与属性匹配表重建。

### 8.5 结构化结果与长期 Reload Hook

注册、注销、重载类 API 提供 `RegisterResult` / `ReloadResult`，用于外部插件判断成功状态和失败原因：

```kotlin
val result = Nodens.reloadHandleResult()
if (!result.success) {
    result.throwable?.printStackTrace()
}
```

外部插件可以注册长期 Reload Hook，避免每次都依赖事件中的临时函数：

```kotlin
Nodens.registerReloadHook("BattleCodex", 0, Runnable {
    // 同步外部插件托管配置并重新注册属性组
})
Nodens.unregisterReloadHooks("BattleCodex")
```

### 8.6 伤害公式提供者

`DamageFormulaProvider` 允许外部插件在 `DamageProcessor.getFinalDamage()` 阶段接管公式：

```kotlin
Nodens.registerDamageFormulaProvider("custom", 10) { processor ->
    if (processor.damageType == "PHYSICAL") 100.0 else null
}
```

提供者按 `priority` 升序执行；返回 `null` 表示不处理，继续尝试下一个提供者，全部返回 `null` 时回退到 `handle.yml`。

### 8.7 Source 属性信息

`Source` 保留原有 `attribute` 名称，同时增加：

- `attributeGroup`
- `attributeName`
- `attributeFullName`

Kether 中可读取 `group`、`attributeName`、`attributeFullName`，用于区分同名属性，如 `Damage:Physics` 与 `Defence:Physics`。

### 8.8 属性刷新命名

`EntityAttributeMemory.updateAttribute()` 是新的明确命名；`refreshAttribute()` 为别名。旧 `updateAttributeAsync()` 保留兼容，但标记为废弃，因为它并不表示整个流程都在异步线程执行。

---

## 9. MythicMobs 兼容层

Nodens 对 MythicMobs 采用双版本 Hook 结构：

| 兼容层 | API 包名 | 说明 |
|--------|----------|------|
| 4.x | `io.lumine.xikage.mythicmobs.*` | 保留旧版 4.11 兼容入口 |
| 5.x | `io.lumine.mythic.*` | 新增 MythicMobs 5.x 事件与技能接口适配 |

### 9.1 版本隔离

`MythicMobsCompatSupport` 会从 Bukkit 插件描述读取 MythicMobs 版本号并解析 major 版本：

- `major >= 5` 时仅执行 5.x Hook。
- `major < 5` 或无法解析但插件已加载时仅执行旧版 Hook。

这样可以避免部分服务端同时存在兼容类时重复注册 `NO-DAMAGE`、重复挂载属性或重复处理死亡掉落。

### 9.2 公共逻辑

版本专用 Hook 只处理 MythicMobs API 差异，公共逻辑集中在 `MythicMobsCompatSupport`：

- 读取 `Nodens` 属性列表并写入 `EntityAttributeMemory`。
- 在出生后同步属性到 Bukkit 并恢复 MythicMob 当前生命。
- 死亡时移除 MythicMobs 属性来源并处理 `nodensDrops`。
- `NO-DAMAGE` 技能统一委托 Nodens `DamageProcessor` 执行伤害计算。

### 9.3 MythicMobs 配置约定

```yaml
ExampleMob:
  Type: ZOMBIE
  Nodens:
    - '物理攻击 10-20'
    - '最大生命 100'
  nodensDrops:
    - 'example 0.5 false 1-2'
  Skills:
    - NO-DAMAGE{type=Physics;power=1.2} @target ~onAttack
```

该配置写法在 MythicMobs 4.x 与 5.x 中保持一致。

---

## 附录: 属性配置示例

### Damage.yml
```yaml
Physics:
  combatPower: 1.0
  valueType: RANGE
  handlePriority: 1
  keys:
    - 物理攻击
```

### Health.yml
```yaml
Max:
  combatPower: 1.0
  valueType: SINGLE
  handlePriority: 1
  syncPriority: 1
  default: 20
  keys:
    - 生命上限
    - 最大生命值

Regain:
  combatPower: 1.0
  valueType: SINGLE
  handlePriority: 1
  period: 20
  keys:
    - 生命恢复
```

### handle.yml (伤害公式)
```yaml
onDamage: |-
  # 基础伤害 + 增伤
  set damage to 0
  for i in &damageSources then {
    case &i[attribute] [
      when "Physics" -> set damage to math add [ &damage &i[value] ]
      when "Magic" -> set damage to math add [ &damage &i[value] ]
      ...
    ]
  }
  # 防御计算
  set defence to 0
  for i in &defenceSources then { ... }
  # 最终公式
  calc "(((damage+addon)*(1-defence/(1000+defence))*1)+enhancement)*scale"
```
