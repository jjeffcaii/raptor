package `as`.leap.raptor.core

import io.vertx.core.Vertx
import io.vertx.core.parsetools.RecordParser
import io.vertx.kotlin.core.net.NetServerOptions
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles


object SimpleNetServer {

  val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())!!

  @JvmStatic
  fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    val options = NetServerOptions(port = 1935)
    val server = vertx.createNetServer(options)
    server.connectHandler({ socket ->
      val parser = RecordParser.newFixed(1, null)
      val handler = BufferHandler(parser, { msg ->
        logger.info("got {} message: {} bytes.", msg.type().name, msg.buffer().length())
      })
      parser.setOutput(handler)
      socket.handler(parser)

      socket.closeHandler({
        logger.info("socket closed.")
      })
      socket.exceptionHandler({ throwable ->
        logger.error("socket error.", throwable)
      })
    })

    server.listen({ result ->
      if (result.succeeded()) {
        logger.info("server start success!")
      }
    })
  }
}