package `as`.leap.raptor.core

import `as`.leap.raptor.core.endpoint.Frontend
import `as`.leap.raptor.core.utils.VertxHelper
import io.vertx.kotlin.core.net.NetServerOptions

object SwapperTest {


  @JvmStatic
  fun main(args: Array<String>) {
    val netServerOptions = NetServerOptions(tcpNoDelay = true, port = 1935)
    val server = VertxHelper.vertx.createNetServer(netServerOptions)
    server.connectHandler({ socket ->
      socket.pause()
      val client: Endpoint = Frontend(socket)
      val swapper = Swapper(client)
      socket.resume()
    })
    server.listen {
      if (it.succeeded()) {
        println("server start success!")
      }
    }
  }
}