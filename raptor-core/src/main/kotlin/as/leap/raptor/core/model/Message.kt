package `as`.leap.raptor.core.model

import io.vertx.core.buffer.Buffer

interface Message<out T> {
  fun type(): MessageType
  fun toBuffer(): Buffer
  fun toModel(): T
}