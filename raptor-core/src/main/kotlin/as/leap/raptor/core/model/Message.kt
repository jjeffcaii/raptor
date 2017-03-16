package `as`.leap.raptor.core.model

import io.vertx.core.buffer.Buffer

abstract class Message<out T>(private val type: MessageType, private val buffer: Buffer, private val clazz: Class<T>) {

  fun type(): MessageType {
    return this.type
  }

  fun buffer(): Buffer {
    return this.buffer
  }

  abstract fun toModel(): T

}