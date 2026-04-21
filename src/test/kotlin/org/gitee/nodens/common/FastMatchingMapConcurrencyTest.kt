package org.gitee.nodens.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger

class FastMatchingMapConcurrencyTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  并发安全
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `多线程并发 put 不崩溃`() {
        val map = FastMatchingMap<String>()
        val threads = 10
        val barrier = CyclicBarrier(threads)
        val errors = AtomicInteger(0)

        val threadList = (0 until threads).map { i ->
            Thread {
                try {
                    barrier.await()
                    for (j in 0 until 100) {
                        map.put("键${i}_$j", "值${i}_$j")
                    }
                } catch (_: Exception) {
                    errors.incrementAndGet()
                }
            }
        }

        threadList.forEach { it.start() }
        threadList.forEach { it.join() }

        assertEquals(0, errors.get(), "并发 put 不应产生异常")
    }

    @Test
    fun `多线程并发 get 不崩溃`() {
        val map = FastMatchingMap<String>()
        // 预填充数据
        for (i in 0 until 100) {
            map.put("属性$i", "值$i")
        }

        val threads = 10
        val barrier = CyclicBarrier(threads)
        val errors = AtomicInteger(0)

        val threadList = (0 until threads).map {
            Thread {
                try {
                    barrier.await()
                    for (i in 0 until 100) {
                        map.get("属性$i+100")
                    }
                } catch (_: Exception) {
                    errors.incrementAndGet()
                }
            }
        }

        threadList.forEach { it.start() }
        threadList.forEach { it.join() }

        assertEquals(0, errors.get(), "并发 get 不应产生异常")
    }

    @Test
    fun `put 和 get 并发不崩溃`() {
        val map = FastMatchingMap<String>()
        val threads = 10
        val barrier = CyclicBarrier(threads)
        val errors = AtomicInteger(0)

        val threadList = (0 until threads).map { i ->
            Thread {
                try {
                    barrier.await()
                    for (j in 0 until 100) {
                        if (i % 2 == 0) {
                            map.put("键${i}_$j", "值${i}_$j")
                        } else {
                            map.get("键${i}_$j+100")
                        }
                    }
                } catch (_: Exception) {
                    errors.incrementAndGet()
                }
            }
        }

        threadList.forEach { it.start() }
        threadList.forEach { it.join() }

        assertEquals(0, errors.get(), "并发 put/get 不应产生异常")
    }

    @Test
    fun `clear 和 get 并发不崩溃`() {
        val map = FastMatchingMap<String>()
        for (i in 0 until 50) {
            map.put("属性$i", "值$i")
        }

        val threads = 10
        val barrier = CyclicBarrier(threads)
        val errors = AtomicInteger(0)

        val threadList = (0 until threads).map { i ->
            Thread {
                try {
                    barrier.await()
                    for (j in 0 until 100) {
                        if (i == 0 && j == 50) {
                            map.clear()
                        } else {
                            map.get("属性${j % 50}+100")
                        }
                    }
                } catch (_: Exception) {
                    errors.incrementAndGet()
                }
            }
        }

        threadList.forEach { it.start() }
        threadList.forEach { it.join() }

        assertEquals(0, errors.get(), "并发 clear/get 不应产生异常")
    }
}
