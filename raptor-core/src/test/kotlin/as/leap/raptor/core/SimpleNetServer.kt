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
      val remote = Backend(platforms.second)
      client
          .onClose { remote.close() }
          .onHandshake {
            val b = it.toBuffer()
            //logger.info(">>> snd handshake: {} bytes", b.length())
            remote.write(b)
          }
          .onChunk {
            val b = it.toBuffer()
            //logger.info(">>> snd chunk({} bytes): fmt={}, csid={} ", b.length(), it.fmt, it.csid)
            remote.write(b)
          }

      val agg = ChunkAggregator()

      remote
          .onClose { client.close() }
          .onHandshake {
            val b = it.toBuffer()
            //logger.info("<<< rcv handshake: {} bytes", b.length())
            client.write(b)
          }
          .onChunk {

            val b = it.toBuffer()
            //logger.info("<<< rcv: {} bytes\n{}\n<<<", b.length(), CodecHelper.encodeHex(b.bytes, true))
            agg.push(it)
            //logger.info("<<< rcv chunk({} bytes): fmt={}, csid={} ", b.length(), it.fmt, it.csid)
            client.write(b)

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