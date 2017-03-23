package `as`.leap.raptor.core.model.payload

import `as`.leap.raptor.core.model.ChunkType
import `as`.leap.raptor.core.model.Payload
import `as`.leap.raptor.core.utils.CodecHelper
import io.vertx.core.buffer.Buffer

class SimpleAMFPayload(val body: Array<Any?>, val type: ChunkType) : Payload {

  override fun toBuffer(): Buffer {
    return Buffer.buffer(CodecHelper.encodeAMF0(this.body))
  }

}