package `as`.leap.raptor.core

import com.google.common.base.Throwables
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
          backend.exceptionHandler {
            logger.error("backend error: {}", Throwables.getStackTraceAsString(it))
          }
          backend.handler {
            //logger.info("rcv toBuffer: {} bytes.", buffer.length())
            front.write(it)
          }
          front.resume()
          val parser = RecordParser.newFixed(1, null)
          val handler = MessageFliper(parser, {
            //logger.info("snd {} message: {} bytes.", it.type(), it.toBuffer().length())
            //logger.info("message model: {}", it.toModel())
            backend.write(it.toBuffer())
          })
          parser.setOutput(handler)
          front.closeHandler {
            logger.info("front socket closed.")
            backend.close()
          }
          front.exceptionHandler {
            logger.error("front socket error.", Throwables.getStackTraceAsString(it))
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