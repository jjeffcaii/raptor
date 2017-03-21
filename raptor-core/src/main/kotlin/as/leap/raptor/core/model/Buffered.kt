package `as`.leap.raptor.core.model

import io.vertx.core.buffer.Buffer

interface Buffered {
  fun toBuffer(): Buffer
}