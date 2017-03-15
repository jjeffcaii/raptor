package `as`.leap.raptor.core

import io.vertx.core.buffer.Buffer

interface Datum {
  fun write(buffer: Buffer): Datum
  fun enough(size: Short): Boolean
  fun buffer(): Buffer
  fun pop(size: Short): Buffer
}