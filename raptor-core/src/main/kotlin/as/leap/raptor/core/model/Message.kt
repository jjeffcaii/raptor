package `as`.leap.raptor.core.model

import io.vertx.core.buffer.Buffer

class Message(private val type: MessageType, private val buffer: Buffer) {

  fun type(): MessageType {
    return this.type
  }

  fun buffer(): Buffer {
    return this.buffer
  }
}