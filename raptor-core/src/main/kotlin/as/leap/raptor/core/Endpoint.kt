package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.Message
import com.google.common.base.Preconditions
import io.vertx.core.buffer.Buffer
import java.io.Closeable

typealias OnMessage = (Message<*>) -> Unit
typealias OnError = (Throwable) -> Unit
typealias OnClose = () -> Unit

abstract class Endpoint : Closeable {

  protected var consumer: OnMessage? = null
  protected var onError: OnError? = null
  protected var onClose: OnClose? = null

  fun onMessage(consumer: OnMessage): Endpoint {
    synchronized(this) {
      Preconditions.checkArgument(this.consumer == null, "message handler exists already!")
      this.consumer = consumer
      return this
    }
  }

  fun onClose(handler: OnClose): Endpoint {
    synchronized(this) {
      Preconditions.checkArgument(this.onClose == null, "close handler exists already!")
      this.onClose = handler
      return this
    }
  }

  fun onError(handler: OnError): Endpoint {
    synchronized(this) {
      Preconditions.checkArgument(this.onError == null, "error handler exists already!")
      this.onError = handler
      return this
    }
  }

  abstract fun write(buffer: Buffer): Endpoint

}