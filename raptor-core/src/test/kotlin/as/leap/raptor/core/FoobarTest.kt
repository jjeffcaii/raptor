package `as`.leap.raptor.core

import `as`.leap.raptor.core.utils.Bytes
import io.vertx.core.buffer.Buffer
import org.testng.annotations.Test
import kotlin.experimental.and

class FoobarTest {

  @Test
  fun test() {
    val buffer = Buffer.buffer(1)
    buffer.appendByte(0x1f)
    buffer.appendByte(0x1f)
    println(buffer.length())
  }

  @Test
  fun test2() {

    val b: Byte = 0x02

    val bb = b and 0x3F

    println(bb)
  }

  @Test
  fun test3() {
    val b = Buffer.buffer().appendByte(0).appendByte(1).appendByte(-51)

    //0x00 0x01 0x01
    println(b.getUnsignedMedium(0))

  }


}