# Nodens

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

- **丰富的属性系统** - 15+ 种属性类型，涵盖生命、伤害、防御、速度、暴击、法力等
- **动态物品生成** - 支持随机属性、Kether 表达式、变量插值
- **战斗系统集成** - 完整的伤害计算、治疗处理、暴击机制
- **多插件兼容** - 原生支持 DragonCore、MythicMobs、DragonArmourers 等
- **高性能缓存** - 基于 Caffeine 的实体属性缓存系统
- **热重载支持** - 无需重启服务器即可重载配置
- **脚本扩展** - 支持 Kether 和 JavaScript 动态脚本

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
```

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
