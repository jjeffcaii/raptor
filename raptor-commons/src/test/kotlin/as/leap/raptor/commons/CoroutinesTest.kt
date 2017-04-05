package `as`.leap.raptor.commons

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.testng.annotations.Test
import kotlin.coroutines.experimental.buildSequence
import kotlin.system.measureTimeMillis

class CoroutinesTest {

  @Test(enabled = false)
  fun testYield() {
    val fib = buildSequence {
      var first: Long = 0L
      var second: Long = 1L
      yield(first)
      yield(second)
      do {
        val sum = first + second
        yield(sum)
        first = second
        second = sum
      } while (true)
    }

    val begin = System.currentTimeMillis()
    var i = 0
    val iter = fib.iterator()
    for (v in iter) {
      if (++i > 100) {
        println("fib $i: $v")
        break
      }
    }
    println("cost: ${System.currentTimeMillis() - begin} ms")
  }

  private suspend fun longTimeJob(): Long {
    val t = RandomUtils.nextLong(1000L, 3000L)
    delay(t)
    return t
  }

  @Test(enabled = false)
  fun testAsyncAwait() {
    runBlocking {
      val cost = measureTimeMillis {
        val a1 = async(CommonPool) { longTimeJob() }
        val a2 = async(CommonPool) { longTimeJob() }
        println("a1+a2: ${a1.await() + a2.await()}")
      }
      println("cost: $cost ms")
    }
  }

}