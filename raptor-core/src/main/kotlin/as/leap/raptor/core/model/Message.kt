package `as`.leap.raptor.core.model

import io.vertx.core.buffer.Buffer

data class Message(val header: Header, val buffer: Buffer) {

  fun <T : Buffered> toModel(clazz: Class<T>): T {
    return clazz.newInstance().from(this.buffer)
  }

}