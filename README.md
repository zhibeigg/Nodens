# Nodens

> 稳定高效的属性插件

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/zhibeigg/Nodens)
## 构建发行版本

[<img src="https://camo.githubusercontent.com/a654761ad31039a9c29df9b92b1dc2be62d419f878bf665c3288f90254d58693/68747470733a2f2f77696b692e70746d732e696e6b2f696d616765732f362f36392f5461626f6f6c69622d706e672d626c75652d76322e706e67" alt="" width="300">](https://github.com/TabooLib/taboolib)

Nodens是免费的, 发行版本用于正常使用, 不含 TabooLib 本体。

```
./gradlew build
```

## 构建开发版本

开发版本包含 TabooLib 本体, 用于开发者使用, 但不可运行。

```
./gradlew taboolibBuildApi -PDeleteCode
```

> 参数 -PDeleteCode 表示移除所有逻辑代码以减少体积。

## 使用API

```
repositories {
    maven("https://www.mcwar.cn/nexus/repository/maven-public/")
}

dependencies {
    compileOnly("org.gitee.nodens:Nodens:{VERSION}:api")
}
```

> {VERSION} 处填写版本号 如 1.0.0

## BStats
[![](https://bstats.org/signatures/bukkit/Nodens.svg)](https://bstats.org/plugin/bukkit/Nodens/25468/)