# Nodens

<p align="center">
  <img src="https://image.mcwar.cn/i/2026/04/23/69ea0b9089e31.png" alt="Nodens Logo" width="600"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.12--1.20+-brightgreen?style=for-the-badge&logo=minecraft" alt="Minecraft Version"/>
  <img src="https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/License-Free-blue?style=for-the-badge" alt="License"/>
  <img src="https://img.shields.io/badge/Version-1.19.30-orange?style=for-the-badge" alt="Version"/>
</p>

<p align="center">
  <b>稳定高效的 Minecraft 属性插件</b><br/>
  为 Bukkit/Paper 服务器提供完整的 RPG 属性系统解决方案
</p>

<p align="center">
  <a href="https://deepwiki.com/zhibeigg/Nodens">
    <img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki"/>
  </a>
</p>

---

## 特性一览

- **Trie 前缀树属性识别** — 基于 `FastMatchingMap` 构建的 Trie 前缀树，O(m) 时间复杂度完成 Lore 属性关键词匹配，ConcurrentHashMap 节点实现无锁并发读写，单节点内存占用从 512KB 压缩至约 1KB
- **PRD 伪随机分布算法** — 移植 DOTA2 的 PRD 概率模型，连续未触发时概率递增，杜绝纯随机下的极端不触发与连续触发，启动时预计算 C 值并持久化缓存，运行时 O(1) 查表
- **全链路攻击流引擎** — 从 Bukkit 事件拦截 → DamageProcessor 构建 → 攻击者/防御者属性按优先级逐层结算 → Kether 脚本驱动伤害公式 → PriorityRunnable 后置回调（吸血等），完整的伤害/恢复双向处理管线
- **属性源追踪溯源** — 每条伤害/防御/恢复来源均持有唯一 Source Key，运行时可通过 Kether 脚本精确查询、动态修改任意来源的贡献值，临时属性支持持续时间、死亡移除与倒计时查询
- **六阶段物品生成流水线** — 变量初始化 → 核心属性计算 → Lore/Name 模板解析 → 物品构建 → NBT 二进制序列化 → 事件触发，全流程 Kether 表达式驱动，`*0*` 标记自动剔除零值行
- **Caffeine 高性能缓存** — 初始容量 200 / 最大 500 条 / 5 分钟 TTL，装备变更事件 50ms 防抖合并，recordStats 命中率监控，避免每 tick 重复解析装备栏 Lore
- **沙箱化 JavaScript 引擎** — Nashorn 预编译 + 独立线程池执行，SafeClassFilter 白名单隔离危险包（io/nio/net/reflect/Runtime），5 秒超时自动熔断，每个 JS 文件即一个自定义属性
- **热重载 & 脚本热更新** — 伤害公式、物品配置、属性定义均可运行时重载，无需重启服务器；Kether 脚本即时生效，Mapping 属性支持一对多动态映射转换

---

## 项目优势

### 🧩 极致可扩展的物品体系

物品系统采用 **触发器 + 条件 + 动作** 三层解耦架构。`ActionTrigger` 通过 ClassVisitor 自动发现注册，外部插件只需实现接口即可注入自定义触发器；`ICondition` 条件接口与 `VariableAdapter` 变量适配器同样支持热插拔扩展。内置 12 种 Variable 类型、`ContextSerializer` 二进制序列化压缩存储，物品上下文在 NBT 中的体积远小于 JSON 方案。`@PluginDepend` 注解实现软依赖自动降级——Orryx 未安装时，相关触发器静默跳过，零侵入零报错。

### 🔬 毫秒级属性源溯源

每个属性变动都携带结构化的 Source 元数据（来源 Key、属性类型、贡献数值），从装备 Lore 解析到临时 Buff 叠加，全部纳入统一的 `EntityAttributeMemory` 管理。开发者可在 Kether 脚本中通过 `source.key` / `source.amount` 实时读写任意来源，也可通过命令行一键查看玩家所有临时属性的来源链路。调试属性冲突、排查数值异常，不再需要逐个插件排查。

### ⚡ 零妥协的性能设计

Trie 前缀树将属性匹配从暴力遍历降维到字符级扫描；Caffeine 缓存 + 50ms 防抖将装备变更的属性重算频率压到最低；PRD 算法预计算 + 文件持久化实现启动零开销；JavaScript 脚本预编译 + 线程池隔离 + 5 秒熔断，杜绝恶意脚本拖垮主线程。属性同步通过 `syncPriority` 排序后批量写入 Bukkit Attribute，减少不必要的 NMS 调用。

### 🛡️ 生产级安全防护

JavaScript 引擎内置 `SafeClassFilter` 白名单，`java.io`、`java.net`、`Runtime`、`ProcessBuilder`、`reflect` 等危险包全部拦截；脚本执行在独立线程池中运行，超时自动取消，主线程零阻塞。Redis 缓存支持 Single/Cluster 双模式，3 小时自动过期，本地 JSON 文件兜底回退，数据链路不存在单点故障。

---

## 属性系统

Nodens 提供了一套完整的 RPG 属性体系：

### 生命属性 (Health)
| 属性             | 说明                |
|----------------|-------------------|
| Max            | 最大生命值（同步到 Bukkit） |
| Regain         | 自然回复              |
| RegainAddon    | 百分比回复加成           |
| Heal           | 治疗强度              |
| GrievousWounds | 重伤效果（减少受到的治疗）     |
| Healer         | 治疗增幅              |

### 伤害属性 (Damage)
| 属性           | 说明           |
|--------------|--------------|
| Physics      | 物理伤害         |
| Magic        | 魔法伤害         |
| Real         | 真实伤害         |
| Fire         | 火焰伤害         |
| Monster/Boss | 怪物/Boss 伤害加成 |

### 防御属性 (Defence)
| 属性      | 说明   |
|---------|------|
| Physics | 物理防御 |
| Magic   | 魔法防御 |
| Fire    | 火焰防御 |

### 速度属性 (Speed)
| 属性     | 说明   | 默认值 |
|--------|------|-----|
| Attack | 攻击速度 | 0.2 |
| Move   | 移动速度 | 0.2 |

### 暴击属性 (Crit)
| 属性            | 说明     |
|---------------|--------|
| PhysicsChance | 物理暴击率  |
| MagicChance   | 魔法暴击率  |
| DamageAddon   | 暴击伤害加成 |
| Resistance    | 暴击抗性   |

### 其他属性
| 属性类型      | 属性           | 说明     |
|-----------|--------------|--------|
| Mana      | Max / Regain | 法力值与回复 |
| Exp       | Bonus        | 经验加成   |
| Luck      | -            | 幸运值    |
| SuckBlood | -            | 吸血效果   |

### 动态属性
- **JavaScript** - 基于 JavaScript 的自定义属性计算
- **Mapping** - 属性键名映射重定向

---

## 物品系统

### 物品配置示例

```yaml
# items/example.yml
村好剑:
  update: true
  material: DIAMOND_SWORD
  name: "&b无敌村好剑"
  lore:
    - "&7━━━━━━━━━━━━━━━━━━"
    - "&e物理攻击: &f+{{ randoms 武器攻击 }}"
    - "&e暴击几率: &f+{{ randoms 暴击几率 }}%"
    - "&7━━━━━━━━━━━━━━━━━━"
    - "&c等级限制: {{ variable level }}"
  variables:
    level: |-
      random2 1 to 10
  itemFlags:
    - HIDE_ENCHANTS
    - HIDE_ATTRIBUTES
  unbreakable: true
  enchantments:
    DAMAGE_ALL: 5
  nbt:
    custom-tag:
      value: 'example'
      nt: string
  # 物品动作
  actions:
    - trigger: "attack"
      action:
        - 'tell color inline "&a攻击触发!"'
    - trigger: "attack,attacked"
      action:
        - 'tell color inline "&e战斗中..."'
    - trigger: "right_click_air,right_click_block"
      action:
        - 'tell color inline "&b右键使用!"'
```

### 物品动作系统

物品动作允许在特定事件触发时执行 Kether 脚本。配置在物品的 `actions` 节下，`trigger` 支持逗号分隔多个触发器 ID。

#### 原版触发器

| 触发器 ID | 说明 | 注入变量 |
|-----------|------|----------|
| `attack` | 攻击实体时 | attacker, damager, entity |
| `attacked` | 被攻击时 | attacker, damager, entity |
| `shoot` | 弓/弩射击时 | bow, consumable, projectile |
| `shield_lift` | 举盾时 | - |
| `left_click_air` | 左键空气 | action |
| `left_click_block` | 左键方块 | action, block |
| `right_click_air` | 右键空气 | action |
| `right_click_block` | 右键方块 | action, block |

#### Orryx 触发器（需安装 Orryx 插件）

| 触发器 ID | 说明 | 注入变量 |
|-----------|------|----------|
| `orryx_press_start` | 按键开始 | skill, pressTick |
| `orryx_press_stop` | 按键停止 | skill, pressTick, maxPressTick |
| `orryx_press_tick` | 按键持续 | skill, period, pressTick, maxPressTick |
| `orryx_damage_pre_attacker` | 伤害前（攻击者） | attacker, defender, damage, damageType |
| `orryx_damage_pre_defender` | 伤害前（防御者） | attacker, defender, damage, damageType |
| `orryx_damage_post_attacker` | 伤害后（攻击者） | attacker, defender, damage, damageType, crit |
| `orryx_damage_post_defender` | 伤害后（防御者） | attacker, defender, damage, damageType, crit |
| `orryx_job_change` | 职业变更 | job |
| `orryx_skill_cast_check` | 技能释放检查 | skill, skillParameter |
| `orryx_skill_cast` | 技能释放 | skill, skillParameter |
| `orryx_skill_level_up` | 技能升级 | skill, upLevel |
| `orryx_skill_level_down` | 技能降级 | skill, downLevel |

### 物品条件系统

| 条件类型  | 说明             |
|-------|----------------|
| Bind  | 物品绑定（所有权限制）    |
| Level | 等级要求           |
| Time  | 使用期限           |
| Job   | 职业限制（需要 Orryx） |
| Slot  | 装备槽位限制         |

### 物品掉落系统

支持自定义掉落率和冷却时间配置。

---

## 命令与权限

### 主命令：`/no` (别名: `/nodens`)

| 命令                                  | 说明         | 权限                      |
|-------------------------------------|------------|-------------------------|
| `/no reload`                        | 重载所有配置     | `nodens.reload`         |
| `/no item manager`                  | 打开物品配置管理器  | `nodens.item.manager`   |
| `/no item give <玩家> <物品> <数量> [变量]` | 给予配置物品     | `nodens.item.give`      |
| `/no item update <玩家>`              | 更新玩家背包中的物品 | `nodens.item.update`    |
| `/no info attribute <玩家>`           | 查看玩家属性     | `nodens.info.attribute` |
| `/no info entity`                   | 查看最近实体属性   | `nodens.info.entity`    |
| `/no info item`                     | 查看手持物品信息   | `nodens.info.item`      |

---

## 配置文件

### config.yml

```yaml
# 调试模式
debug: false

# 生命值缩放（同步到 Bukkit 最大生命值）
healthScaled: true

# 物品掉落配置
drop:
  cancel: false           # 是否取消原版掉落
  survival: PT3M          # 掉落冷却时间

# 物品条件关键词配置
condition:
  bind:
    keywords: ['绑定于']
  level:
    keywords: ['等级限制']
  time:
    keywords: ['使用期限']
    pattern: '-'
  job:
    keywords: ['职业限制']
  slot:
    keywords: ['物品类型']
    pattern:
      main-hand: ['主手']
      off-hand: ['副手']
      helmet: ['头盔']
      chestplate: ['胸甲']
      leggings: ['护腿']
      boots: ['鞋子']

# DragonCore 槽位配置
update-dragoncore-slots: []
attribute-dragoncore-slots: []
```

### handle.yml（伤害/治疗公式）

```yaml
# 伤害计算公式（Kether 脚本）
onDamage: |-
  # 自定义伤害计算逻辑

# 治疗计算公式（Kether 脚本）
onRegain: |-
  # 自定义治疗计算逻辑
```

### 属性配置（attribute/*.yml）

```yaml
# 示例：Speed.yml
Attack:
  combatPower: 1.0          # 战斗力系数
  valueType: SINGLE         # 值类型：SINGLE 或 RANGE
  handlePriority: 1         # 处理优先级
  syncPriority: 1           # 同步优先级
  default: 0.2              # 默认值
  keys:
    - '攻击速度'
    - 'Attack Speed'

Move:
  combatPower: 1.0
  valueType: SINGLE
  handlePriority: 1
  syncPriority: 1
  default: 0.2
  keys:
    - '移动速度'
    - 'Move Speed'
```

---

## 插件兼容

| 插件                  | 功能           |
|---------------------|--------------|
| **DragonCore**      | 装备槽位、UI 集成   |
| **DragonArmourers** | 盔甲皮肤系统       |
| **MythicMobs**      | 自定义怪物属性、伤害机制 |
| **GlowAPI**         | 发光效果（可选）     |
| **Orryx**           | 职业系统集成       |

---

## API 使用

### Maven / Gradle 依赖

```kotlin
repositories {
    maven("https://www.mcwar.cn/nexus/repository/maven-public/")
}

dependencies {
    compileOnly("org.gitee.nodens:Nodens:{VERSION}:api")
}
```

> 将 `{VERSION}` 替换为版本号，如 `1.19.30`

### API 示例

```kotlin
import org.gitee.nodens.api.Nodens

// 获取玩家属性
val api = Nodens.api()
val playerAttributes = api.getEntityAttributeMemory(player)

// 获取物品 API
val itemApi = Nodens.itemApi()
val item = itemApi.generateItem("村好剑", mapOf("level" to "5"))
```

---

## 构建指南

### 构建发行版本

发行版本用于正常服务器运行，不含 TabooLib 本体。

```bash
./gradlew build
```

### 构建开发版本

开发版本包含 TabooLib 本体，用于 API 开发，但不可直接运行。

```bash
./gradlew taboolibBuildApi -PDeleteCode
```

> 参数 `-PDeleteCode` 表示移除所有逻辑代码以减少体积。

---

## 技术特性

- **属性匹配系统** - FastMatchingMap 实现 O(1) 属性关键词匹配
- **高性能缓存** - 基于 Caffeine 的实体属性缓存（5 分钟 TTL）
- **异步处理** - 基于协程的异步属性更新
- **伤害引擎** - DamageProcessor 支持伤害/防御源和缩放
- **生命同步** - EntitySyncProfile 实现 Bukkit 属性同步
- **PRD 算法** - 伪随机分布算法计算暴击
- **NBT 支持** - 完整的物品 NBT 数据操作
- **热重载** - 带权重的重载系统，保持插件状态
- **脚本支持** - Kether（高级）和 JavaScript（动态）脚本引擎

---

## 统计数据

[![BStats](https://bstats.org/signatures/bukkit/Nodens.svg)](https://bstats.org/plugin/bukkit/Nodens/25468/)

---

## 依赖框架

Nodens 基于 [TabooLib](https://github.com/TabooLib/taboolib) 6.2 构建。

---

## 许可协议

Nodens 是免费开源软件，欢迎贡献代码和提出建议。

---

<p align="center">
  <b>Made with ❤️ for the Minecraft community</b>
</p>
