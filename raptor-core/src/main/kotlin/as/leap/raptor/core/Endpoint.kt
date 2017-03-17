package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.Message
import io.vertx.core.buffer.Buffer

abstract class Endpoint(protected val consumer: (Message<*>) -> Unit, protected val onErr: ((Throwable) -> Unit)? = null) {

  abstract fun write(buffer: Buffer): Endpoint

}