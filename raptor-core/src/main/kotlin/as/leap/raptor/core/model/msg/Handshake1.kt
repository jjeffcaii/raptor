package `as`.leap.raptor.core.model.msg

import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.MessageType
import io.vertx.core.buffer.Buffer

class Handshake1(private val buffer: Buffer) : Message<Handshake1.Body> {

  override fun toBuffer(): Buffer {
    return this.buffer
  }

  override fun type(): MessageType {
    return MessageType.HANDSHAKE
  }

  private val model: Body by lazy {
    val v1 = this.toBuffer().getUnsignedInt(0)
    val v2 = this.toBuffer().getUnsignedInt(4)
    val random = this.toBuffer().slice(4, 1528)
    Body(v1, v2, random)
  }

  override fun toModel(): Body {
    return this.model
  }

  inner class Body(val v1: Long, val v2: Long, val random: Buffer) {

  }


}