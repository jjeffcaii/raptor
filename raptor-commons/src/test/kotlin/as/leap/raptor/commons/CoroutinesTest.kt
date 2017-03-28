package `as`.leap.raptor.commons

import org.testng.annotations.Test
import kotlin.coroutines.experimental.buildSequence

class CoroutinesTest {

  @Test
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
        break
      }
      println("fib $i: $v")
    }
    println("cost: ${System.currentTimeMillis() - begin} ms")
  }

  suspend fun test1(): Int {
    Thread.sleep(3000L)
    return 1
  }

  @Test
  fun foo() {


  }

}