package me.zarafa.raptor.core.model.payload

import me.zarafa.raptor.core.model.MessageType
import me.zarafa.raptor.core.model.Payload
import io.vertx.core.buffer.Buffer

class SimpleBinaryPayload(val bytes: ByteArray, val type: MessageType) : Payload {

  override fun toBuffer(): Buffer {
    return Buffer.buffer(this.bytes)
  }

  override fun toString(): String {
    return "${type.name}_PAYLOAD (${this.bytes.size} bytes)"
  }

}