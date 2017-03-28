package `as`.leap.raptor.core.model.payload

import `as`.leap.raptor.core.model.MessageType
import `as`.leap.raptor.core.model.Payload
import `as`.leap.raptor.core.utils.Codecs
import io.vertx.core.buffer.Buffer

class SimpleAMFPayload(val body: Array<Any?>, val type: MessageType) : Payload {

  override fun toBuffer(): Buffer {
    return Buffer.buffer(Codecs.encodeAMF0(this.body))
  }

}