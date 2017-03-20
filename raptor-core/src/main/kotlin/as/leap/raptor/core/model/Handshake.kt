package `as`.leap.raptor.core.model

import io.vertx.core.buffer.Buffer

data class Handshake(val buffer: Buffer) {

  fun toBuffer(): Buffer {
    return this.buffer
  }

}