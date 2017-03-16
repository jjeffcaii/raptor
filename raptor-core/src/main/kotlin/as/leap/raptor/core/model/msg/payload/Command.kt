package `as`.leap.raptor.core.model.msg.payload

import `as`.leap.raptor.core.model.ChunkType
import `as`.leap.raptor.core.model.msg.Payload

abstract class Command(private val type: ChunkType, val cmd: String, val transId: Int, val cmdObj: Any? = null) : Payload {
  override fun type(): ChunkType {
    return this.type
  }
}