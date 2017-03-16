package `as`.leap.raptor.core.model.msg

import `as`.leap.raptor.core.model.Header
import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.MessageType
import io.vertx.core.buffer.Buffer

class Chunk(buffer: Buffer, private val header: Header?) : Message<Chunk.Body>(MessageType.CHUNK, buffer, Body::class.java) {

  private val model: Body by lazy {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun toModel(): Body {
    return this.model
  }

  inner class Body(header: Header) {

  }

}