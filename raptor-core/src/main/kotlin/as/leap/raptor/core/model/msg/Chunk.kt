package `as`.leap.raptor.core.model.msg

import `as`.leap.raptor.core.model.Header
import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.MessageType
import io.vertx.core.buffer.Buffer

class Chunk(private val buffer: Buffer, private val header: Header?) : Message<Chunk.Body> {

  override fun toBuffer(): Buffer {
    return this.buffer
  }

  override fun type(): MessageType {
    return MessageType.CHUNK
  }

  private val model: Body by lazy {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun toModel(): Body {
    return this.model
  }

  inner class Body(header: Header) {

  }

}