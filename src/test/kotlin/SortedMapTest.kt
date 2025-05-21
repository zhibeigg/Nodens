import org.gitee.nodens.util.comparePriority

object SortedMapTest {

    private val testMap = mapOf(TestSort(0) to mapOf("test" to "1"), TestSort(0) to mapOf("test" to "4"), TestSort(1) to mapOf("test" to "3"), TestSort(5) to mapOf("test" to "2"))

    class TestSort(val sort: Int)

    @JvmStatic
    fun main(args: Array<String>) {
        testMap.toSortedMap { o1, o2 ->
            comparePriority(o1.sort, o2.sort)
        }.forEach {
            println(it.key.sort)
            println(it.value)
        }
    }
}