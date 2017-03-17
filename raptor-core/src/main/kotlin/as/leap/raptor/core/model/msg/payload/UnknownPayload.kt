package `as`.leap.raptor.core.model.msg.payload

import `as`.leap.raptor.core.model.ChunkType
import `as`.leap.raptor.core.model.msg.Payload

class UnknownPayload : Payload {

  override fun type(): ChunkType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  companion object {
    val INSTANCE: Payload by lazy {
      UnknownPayload()
    }
  }

  override fun toString(): String {
    return "<UNKNOWN_PAYLOAD>"
  }

}