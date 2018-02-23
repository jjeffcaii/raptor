package `in`.firedog.raptor.core.utils

import io.vertx.core.buffer.Buffer

interface Buffered {
  fun toBuffer(): Buffer
}