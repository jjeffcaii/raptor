package `as`.leap.raptor.core.model.msg.payload

import `as`.leap.raptor.core.model.ChunkType
import `as`.leap.raptor.core.model.msg.Payload

class UnknownPayload(private val type: ChunkType) : Payload {

  override fun type(): ChunkType {
    return this.type
  }

  override fun toString(): String {
    return "<${this.type.name}_PAYLOAD>"
  }

}