package `as`.leap.raptor.core.ext

import `as`.leap.raptor.core.model.Chunk
import `as`.leap.raptor.core.model.Handshake
import `as`.leap.raptor.core.utils.Callback
import com.google.common.base.Preconditions
import io.vertx.core.buffer.Buffer
import java.io.Closeable


abstract class Endpoint : Closeable {

  protected var onHandshake: Callback<Handshake>? = null
  protected var onChunk: Callback<Chunk>? = null
  protected var onError: Callback<Throwable>? = null
  protected var onClose: Callback<Unit>? = null

  fun onHandshake(consumer: Callback<Handshake>): Endpoint {
    synchronized(this) {
      Preconditions.checkArgument(this.onHandshake == null, "handshake handler exists already!")
      this.onHandshake = consumer
      return this
    }
  }

  fun onChunk(consumer: Callback<Chunk>): Endpoint {
    synchronized(this) {
      Preconditions.checkArgument(this.onChunk == null, "chunk handler exists already!")
      this.onChunk = consumer
      return this
    }
  }

  fun onClose(handler: Callback<Unit>): Endpoint {
    synchronized(this) {
      Preconditions.checkArgument(this.onClose == null, "close handler exists already!")
      this.onClose = handler
      return this
    }
  }

  fun onError(handler: Callback<Throwable>): Endpoint {
    synchronized(this) {
      Preconditions.checkArgument(this.onError == null, "error handler exists already!")
      this.onError = handler
      return this
    }
  }

  abstract fun write(buffer: Buffer): Endpoint

}