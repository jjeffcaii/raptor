package me.zarafa.raptor.core.model.payload

import me.zarafa.raptor.core.model.MessageType
import me.zarafa.raptor.core.model.Payload
import me.zarafa.raptor.core.utils.Codecs
import io.vertx.core.buffer.Buffer

class SimpleAMFPayload(val body: Array<Any?>, val type: MessageType) : Payload {

  override fun toBuffer(): Buffer {
    return Buffer.buffer(Codecs.encodeAMF0(this.body))
  }

}