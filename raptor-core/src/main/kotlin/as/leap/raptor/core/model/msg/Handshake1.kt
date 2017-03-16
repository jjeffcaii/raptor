package `as`.leap.raptor.core.model.msg

import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.MessageType
import io.vertx.core.buffer.Buffer

class Handshake1(buffer: Buffer) : Message<Handshake1.Body>(MessageType.HANDSHAKE, buffer, Body::class.java) {

  private val model: Body by lazy {
    val v1 = this.buffer().getUnsignedInt(0)
    val v2 = this.buffer().getUnsignedInt(4)
    val random = this.buffer().slice(4, 1528)
    Body(v1, v2, random)
  }

  override fun toModel(): Body {
    return this.model
  }

  inner class Body(val v1: Long, val v2: Long, val random: Buffer) {

  }


}