package `as`.leap.raptor.core.impl.endpoint

import `as`.leap.raptor.core.impl.ext.ChunkFliper
import `as`.leap.raptor.core.impl.ext.Endpoint
import `as`.leap.raptor.core.utils.Vertxes
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import io.vertx.core.parsetools.RecordParser
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.*

class LazyEndpoint(host: String, port: Int = 1935) : Endpoint() {

  private var socket: NetSocket? = null
  private var queue: MutableList<Buffer> = mutableListOf()

  init {
    Vertxes.netClient.connect(port, host, {
      if (it.succeeded()) {
        val socket = it.result()
        val parser = RecordParser.newFixed(1, null)
        val fliper = ChunkFliper(parser, { this.onHandshake!!.invoke(it) }, { this.onChunk!!.invoke(it) })
        parser.setOutput(fliper)
        socket.handler(parser)
        socket.exceptionHandler {
          logger.error("endpoint error.", it)
          this.onError?.invoke(it)
        }
        socket.closeHandler {
          if (logger.isDebugEnabled) {
            logger.debug("endpoint closed.")
          }
          this.onClose?.invoke(Unit)
          this.socket = null
        }
        synchronized(this) {
          this.queue.forEach {
            socket.write(it)
          }
          this.queue = Collections.emptyList()
        }
        this.socket = socket

        if (logger.isDebugEnabled) {
          logger.debug("initialize endpoint success.")
        }
      } else {
        logger.error("initialize endpoint failed.", it.cause())
        this.onError?.invoke(it.cause())
      }
    })
  }

  override fun write(buffer: Buffer): Endpoint {
    if (this.socket != null) {
      this.socket!!.write(buffer)
      return this
    }
    synchronized(this) {
      if (this.socket == null) {
        this.queue.add(buffer)
      }
      this.socket?.write(buffer)
    }
    return this
  }

  override fun close() {
    synchronized(this) {
      if (this.socket != null) {
        this.socket!!.close()
        this.socket = null
      }
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }
}
