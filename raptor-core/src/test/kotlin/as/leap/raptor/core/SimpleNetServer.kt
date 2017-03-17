package `as`.leap.raptor.core

import `as`.leap.raptor.core.endpoint.BackendEndpoint
import `as`.leap.raptor.core.endpoint.FrontEndpoint
import `as`.leap.raptor.core.utils.VertxHelper
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles


object SimpleNetServer {

  private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())!!

  @JvmStatic
  fun main(args: Array<String>) {
    val platforms = mapOf(
        "qiniu" to "pili-live-rtmp.maxwon.cn",
        "douyu" to "send3.douyu.com"
    )
    val server = VertxHelper.vertx.createNetServer()

    server.connectHandler({ socket ->
      socket.pause()
      val client = FrontEndpoint(socket)
      val remote = BackendEndpoint(platforms["qiniu"]!!, 1935)
      client.onClose { remote.close() }.onMessage {
        logger.info(">>> snd message: {}", it.toModel())
        remote.write(it.toBuffer())
      }
      remote.onClose { client.close() }.onMessage {
        logger.info("<<< rcv message: {}", it.toModel())
        client.write(it.toBuffer())
      }
      socket.resume()
    })

    server.listen(1935, {
      if (it.succeeded()) {
        logger.info("server start success!")
      } else {
        logger.error("server start failed.", it.cause())
      }
    })
  }
}