package `as`.leap.raptor.core.endpoint

import `as`.leap.raptor.core.Endpoint
import `as`.leap.raptor.core.MessageFliper
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import io.vertx.core.parsetools.RecordParser
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class FrontEndpoint(private val socket: NetSocket) : Endpoint() {

  init {
    val parser = RecordParser.newFixed(1, null)
    parser.setOutput(MessageFliper(parser, {
      this.consumer?.invoke(it)
    }))
    this.socket.handler(parser)
    this.socket.closeHandler {
      this.onClose?.invoke()
      logger.info("endpoint closed.")
    }
    this.socket.exceptionHandler {
      logger.error("endpoint socket error.", it)
      this.onError?.invoke(it)
    }
    logger.info("initialize endpoint success.")
  }

  override fun write(buffer: Buffer): Endpoint {
    this.socket.write(buffer)
    return this
  }

  override fun close() {
    this.socket.close()
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

}