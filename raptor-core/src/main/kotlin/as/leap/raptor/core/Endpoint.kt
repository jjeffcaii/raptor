package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.Message
import io.vertx.core.buffer.Buffer

abstract class Endpoint(private val consumer: (Message<Any>) -> Unit) {

  abstract fun write(buffer: Buffer): Endpoint

}