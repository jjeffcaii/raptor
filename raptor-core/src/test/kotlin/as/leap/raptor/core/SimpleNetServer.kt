package `as`.leap.raptor.core

import `as`.leap.raptor.core.endpoint.Backend
import `as`.leap.raptor.core.endpoint.Frontend
import `as`.leap.raptor.core.utils.VertxHelper
import io.vertx.kotlin.core.net.NetServerOptions
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles


object SimpleNetServer {

  private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())!!

  @JvmStatic
  fun main(args: Array<String>) {
    val platforms = Pair("pili-live-rtmp.maxwon.cn", "send3.douyu.com")
    val netServerOptions = NetServerOptions(tcpNoDelay = true, port = 1935)
    val server = VertxHelper.vertx.createNetServer(netServerOptions)
    server.connectHandler({ socket ->
      socket.pause()
      val client = Frontend(socket)
      val remote = Backend(platforms.first)
      client.onClose { remote.close() }.onMessage {
        //logger.info(">>> snd message: {}", it.toModel())
        remote.write(it.toBuffer())
      }
      remote.onClose { client.close() }.onMessage {
        //logger.info("<<< rcv message: {}", it.toModel())
        client.write(it.toBuffer())
      }
      socket.resume()
    })

    server.listen {
      if (it.succeeded()) {
        logger.info("server start success!")
      } else {
        logger.error("server start failed.", it.cause())
      }
    }
  }
}