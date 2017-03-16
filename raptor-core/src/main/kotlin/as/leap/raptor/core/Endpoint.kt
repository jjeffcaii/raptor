package `as`.leap.raptor.core

import io.vertx.core.buffer.Buffer

interface Endpoint {

  fun write(buffer: Buffer): Endpoint

}