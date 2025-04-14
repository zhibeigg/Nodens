import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object ReloadLazy {

    private class ReloadableLazy<T>(private val check: () -> Any?, private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
        private var cached: T? = null
        private var initialized: Boolean = false
        private var lastHash: Int? = null

        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val current = check()
            val currentHash = current.hashCode()
            if (!initialized || lastHash != currentHash) {
                cached = initializer()
                initialized = true
                lastHash = currentHash
            }
            @Suppress("UNCHECKED_CAST")
            return cached as T
        }
    }

    private var checkState = ""
    private var reloadTrigger = "1"

    private val myLazyProperty: String by ReloadableLazy({ checkState }) { reloadTrigger }

    @JvmStatic
    fun main(args: Array<String>) {
        println(myLazyProperty)  // 输出: 1
        reloadTrigger = "2"
        println(myLazyProperty)  // 输出: 1
        checkState = "trigger"
        println(myLazyProperty)  // 输出: 2
    }
}