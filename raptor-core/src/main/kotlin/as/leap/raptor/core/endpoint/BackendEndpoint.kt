package `as`.leap.raptor.core.endpoint

import `as`.leap.raptor.core.Endpoint
import `as`.leap.raptor.core.MessageFliper
import `as`.leap.raptor.core.utils.VertxHelper
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import io.vertx.core.parsetools.RecordParser
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.*

class BackendEndpoint(host: String, port: Int = 1935) : Endpoint() {

  private var socket: NetSocket? = null
  private var queue: MutableList<Buffer> = mutableListOf()

  init {
    VertxHelper.netClient.connect(port, host, {
      if (it.succeeded()) {

        val socket = it.result()
        val parser = RecordParser.newFixed(1, null)
        parser.setOutput(MessageFliper(parser, {
          this.consumer?.invoke(it)
        }))
        socket.handler(parser)

        socket.exceptionHandler {
          logger.error("endpoint error.", it)
          this.onError?.invoke(it)
        }

        socket.closeHandler {
          logger.info("endpoint closed.")
          this.onClose?.invoke()
          this.socket = null
        }

        synchronized(this.queue, {
          this.queue.forEach {
            socket.write(it)
          }
        })
        this.queue = Collections.emptyList()
        this.socket = socket
        logger.info("initialize endpoint success.")
      } else {
        logger.error("initialize endpoint failed.", it.cause())
        this.onError?.invoke(it.cause())
      }
    })
  }

  override fun write(buffer: Buffer): Endpoint {
    if (this.socket != null) {
      this.socket!!.write(buffer)
    } else {
      synchronized(this.queue, {
        this.queue.add(buffer)
      })
    }
    return this
  }

  override fun close() {
    this.socket?.close()
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }
}
