package me.zarafa.raptor.core

import me.zarafa.raptor.core.model.FMT
import me.zarafa.raptor.core.model.Header
import me.zarafa.raptor.core.model.MessageType
import me.zarafa.raptor.core.model.SimpleMessage
import me.zarafa.raptor.core.utils.Codecs
import io.vertx.core.buffer.Buffer
import org.testng.Assert
import org.testng.annotations.Test

class MessageTest {

  @Test
  fun test() {
    val buffer = Buffer.buffer().appendInt(1).appendInt(2).appendInt(3).appendInt(4)
    val header = Header(FMT.F0, 3, MessageType.COMMAND_AMF0, 16)
    val msg = SimpleMessage(header, buffer)
    val chunks = msg.toChunks(4)
    Assert.assertEquals(4, chunks.size)
    var i = 0
    chunks.forEach {
      Assert.assertEquals(++i, it.payload.getInt(0))
    }
    val chunks3 = msg.toChunks(3)
    Assert.assertEquals(6, chunks3.size)
    val b = Buffer.buffer()
    chunks3.forEach { b.appendBuffer(it.payload) }
    Assert.assertEquals(16, b.length())
    Assert.assertEquals("00000001000000020000000300000004", Codecs.encodeHex(b.bytes))
    var ck = msg.toChunks(16)
    Assert.assertEquals(1, ck.size)
    Assert.assertEquals("00000001000000020000000300000004", Codecs.encodeHex(ck.first().payload.bytes))

    ck = msg.toChunks(128)
    Assert.assertEquals(1, ck.size)
    Assert.assertEquals("00000001000000020000000300000004", Codecs.encodeHex(ck.first().payload.bytes))
  }

}
