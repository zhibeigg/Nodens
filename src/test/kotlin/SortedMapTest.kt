import kotlin.reflect.KProperty

object SortedMapTest {

    private val testMap = mapOf(TestSort(0) to "test", TestSort(2) to "test", TestSort(1) to "test", TestSort(5) to "test")

    class TestSort(val sort: Int)

    @JvmStatic
    fun main(args: Array<String>) {
        testMap.toSortedMap { o1, o2 ->
            o1.sort.compareTo(o2.sort)
        }.forEach {
            println(it.key.sort)
        }
    }
}