package `as`.leap.raptor.core.model

import io.vertx.core.buffer.Buffer

interface Buffered {

  fun <T : Buffered> from(buffer: Buffer): T

}