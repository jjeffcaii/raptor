package `as`.leap.raptor.core

import io.vertx.core.Vertx
import io.vertx.core.parsetools.RecordParser
import io.vertx.kotlin.core.net.NetClientOptions
import io.vertx.kotlin.core.net.NetServerOptions
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles


object SimpleNetServer {

  private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())!!
  private val clientOptions = NetClientOptions(connectTimeout = 5000)
  private val serverOptions = NetServerOptions(port = 1935)

  private val vertx = Vertx.vertx()
  private val server = vertx.createNetServer(serverOptions)
  private val client = vertx.createNetClient(this.clientOptions)

  @JvmStatic
  fun main(args: Array<String>) {
    server.connectHandler({ front ->
      front.pause()
      client.connect(1935, "pili-live-rtmp.maxwon.cn", { event ->
        if (event.succeeded()) {
          val backend = event.result()
          backend.closeHandler {
            logger.info("backend closed.")
            front.close()
          }
          backend.exceptionHandler { throwable ->
            logger.error("backend error.", throwable)
          }
          backend.handler { buffer ->
            logger.info("rcv buffer: {} bytes.", buffer.length())
            front.write(buffer)
          }
          front.resume()
          val parser = RecordParser.newFixed(1, null)
          val handler = MessageFliper(parser, { msg ->
            logger.info("snd {} message: {} bytes.", msg.type().name, msg.buffer().length())
            backend.write(msg.buffer())
          })
          parser.setOutput(handler)
          front.closeHandler {
            logger.info("socket closed.")
            backend.close()
          }
          front.exceptionHandler { throwable ->
            logger.error("socket error.", throwable)
          }
          front.handler(parser)
        } else {
          front.close()
        }
      })
    })

    server.listen({ result ->
      if (result.succeeded()) {
        logger.info("server start success!")
      }
    })
  }
}