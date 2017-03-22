package `as`.leap.raptor.core.model.payload

import `as`.leap.raptor.core.model.ChunkType
import `as`.leap.raptor.core.model.Payload
import io.vertx.core.buffer.Buffer

class UnknownPayload(private val type: ChunkType) : Payload {

  override fun toBuffer(): Buffer {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun toString(): String {
    return "<${this.type.name}_PAYLOAD>"
  }

}