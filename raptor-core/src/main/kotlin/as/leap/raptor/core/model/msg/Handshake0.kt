package `as`.leap.raptor.core.model.msg

import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.MessageType
import com.google.common.base.MoreObjects
import io.vertx.core.buffer.Buffer

class Handshake0(private val buffer: Buffer) : Message<Handshake0.Body> {

  override fun type(): MessageType {
    return MessageType.HANDSHAKE
  }

  override fun toBuffer(): Buffer {
    return this.buffer
  }

  private val model: Body by lazy {
    Body(this.toBuffer().getByte(0).toShort())
  }

  override fun toModel(): Body {
    return this.model
  }

  inner class Body(val version: Short = 3) {

    override fun toString(): String {
      return MoreObjects.toStringHelper(this)
          .add("version", this.version)
          .toString()
    }

  }

}