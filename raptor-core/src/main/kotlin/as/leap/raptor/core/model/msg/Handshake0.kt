package `as`.leap.raptor.core.model.msg

import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.MessageType
import io.vertx.core.buffer.Buffer

class Handshake0(buffer: Buffer) : Message<Handshake0.Body>(MessageType.HANDSHAKE, buffer, Body::class.java) {

  private val model: Body by lazy {
    Body(this.buffer().getByte(0).toShort())
  }

  override fun toModel(): Body {
    return this.model
  }

  inner class Body(val version: Short = 3)

}