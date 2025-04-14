object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val list = mutableListOf<Int>()
        for (i in 9..44) {
            list.add(i)
        }
        println(list.joinToString(","))
    }

}
