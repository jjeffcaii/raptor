package `in`.firedog.raptor.core.model.payload

import `in`.firedog.raptor.core.model.MessageType
import `in`.firedog.raptor.core.model.Payload
import `in`.firedog.raptor.core.utils.Codecs
import io.vertx.core.buffer.Buffer

class SimpleAMFPayload(val body: Array<Any?>, val type: MessageType) : Payload {

  override fun toBuffer(): Buffer {
    return Buffer.buffer(Codecs.encodeAMF0(this.body))
  }

}