package `as`.leap.raptor.core

import `as`.leap.raptor.core.swapper.OBSSwapper
import `as`.leap.raptor.core.utils.VertxHelper
import `as`.leap.raptor.service.SimpleNamespaceManager
import io.vertx.kotlin.core.net.NetServerOptions

object SwapperTest {

  @JvmStatic
  fun main(args: Array<String>) {
    val manager = SimpleNamespaceManager()
    val netServerOptions = NetServerOptions(tcpNoDelay = true, port = 1935)
    val server = VertxHelper.vertx.createNetServer(netServerOptions)
    server.connectHandler { OBSSwapper(it, manager) }
    server.listen {
      if (it.succeeded()) {
        println("server start success!")
      }
    }
  }
}