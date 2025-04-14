import kotlin.system.measureNanoTime

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val map = LoreMap<Attribute>(ignoreSpace = true, ignoreColor = true, ignoreColon = true, ignorePrefix = true)
        map.put("物理伤害", Damage)
        map.put("生命提升", Health)
        map.put("移动速度", Speed)
        map.put("攻击速度", AttackSpeed)
        map.put("物理防御", Defence)

        val fastMap = FastMatchingMap<Attribute>(ignoreSpace = true, ignoreColor = true, ignoreColon = true, ignorePrefix = true)
        fastMap.put("物理伤害", Damage)
        fastMap.put("生命提升", Health)
        fastMap.put("移动速度", Speed)
        fastMap.put("攻击速度", AttackSpeed)
        fastMap.put("物理防御", Defence)

        val lore = listOf(
            "&a测试&e物理伤害：+99",
            "&a测试生命提升：99",
            "&a测试&e移动速度：+99",
            "&a测试&e攻击速度：-19",
            "&a测试&e物理伤害：+99",
            "&a测试&e攻击速度：+99%",
            "&a测试&e物理防御：+29",
            "&a测试&e物理伤害：+39"
        )

        val timeAvg0 = measureNanoTime {
            for (i in 1..100_0000) {
                lore.forEach {
                    map.getMatchResult(it)
                }
            }
        }
        println(timeAvg0/100_0000)

        val timeAvg1 = measureNanoTime {
            for (i in 1..100_0000) {
                lore.forEach {
                    fastMap.getMatchResult(it)
                }
            }
        }
        println(timeAvg1/100_0000)
    }

    interface Attribute

    object Damage: Attribute
    object Health: Attribute
    object Speed: Attribute
    object AttackSpeed: Attribute
    object Defence: Attribute

}