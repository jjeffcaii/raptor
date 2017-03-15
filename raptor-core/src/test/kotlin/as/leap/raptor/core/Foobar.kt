package `as`.leap.raptor.core

import io.vertx.core.Vertx
import io.vertx.kotlin.core.net.NetServerOptions
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

object Foobar {

  val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())!!

  @JvmStatic
  fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    val options = NetServerOptions(port = 1935)
    val server = vertx.createNetServer(options)
    server.connectHandler({ socket ->
      socket.handler({ buffer ->
        logger.info("income buffer: {}", buffer.length())
      })
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