package `as`.leap.raptor.core.endpoint

import `as`.leap.raptor.core.Endpoint
import `as`.leap.raptor.core.MessageFliper
import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.utils.VertxHelper
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import io.vertx.core.parsetools.RecordParser
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.*

typealias Consumer = (Message<*>) -> Unit
typealias OnError = (Throwable) -> Unit

class BackendEndpoint(host: String, port: Int = 1935, consumer: Consumer, onError: OnError? = null) : Endpoint(consumer, onError) {

  private var socket: NetSocket? = null
  private var queue: MutableList<Buffer> = mutableListOf()

  init {
    VertxHelper.netClient.connect(port, host, {
      if (it.succeeded()) {
        logger.info("create backend socket success.")
        val socket = it.result()
        val parser = RecordParser.newFixed(1, null)
        parser.setOutput(MessageFliper(parser, this.consumer))
        socket.handler(parser)
        synchronized(this.queue, {
          this.queue.forEach {
            socket.write(it)
          }
        })
        this.queue = Collections.emptyList()
        this.socket = socket
      } else {
        logger.error("create backend socket failed.", it.cause())
        this.onErr?.invoke(it.cause())
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

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }
}
