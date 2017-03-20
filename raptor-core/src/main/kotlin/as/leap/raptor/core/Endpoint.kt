package `as`.leap.raptor.core

import `as`.leap.raptor.core.utils.OnChunk
import `as`.leap.raptor.core.utils.OnClose
import `as`.leap.raptor.core.utils.OnError
import `as`.leap.raptor.core.utils.OnHandshake
import com.google.common.base.Preconditions
import io.vertx.core.buffer.Buffer
import java.io.Closeable


abstract class Endpoint : Closeable {

  protected var onHandshake: OnHandshake? = null
  protected var onChunk: OnChunk? = null
  protected var onError: OnError? = null
  protected var onClose: OnClose? = null

  fun onHandshake(consumer: OnHandshake): Endpoint {
    synchronized(this) {
      Preconditions.checkArgument(this.onChunk == null, "handshake handler exists already!")
      this.onHandshake = consumer
      return this
    }
  }

  fun onChunk(consumer: OnChunk): Endpoint {
    synchronized(this) {
      Preconditions.checkArgument(this.onChunk == null, "chunk handler exists already!")
      this.onChunk = consumer
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