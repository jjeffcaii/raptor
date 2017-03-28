package `as`.leap.raptor.core

import `as`.leap.raptor.core.impl.OBSSwapper
import `as`.leap.raptor.core.utils.Vertxes
import `as`.leap.raptor.service.SimpleNamespaceManager
import io.vertx.core.Vertx
import io.vertx.kotlin.core.net.NetServerOptions

object SwapperTest {

  private val vertx = Vertx.vertx()
  private val netClient = vertx.createNetClient()
  private val manager = SimpleNamespaceManager()//MockNamespaceManager()

  @JvmStatic
  fun main(args: Array<String>) {
    val netServerOptions = NetServerOptions(tcpNoDelay = true, port = 1935)
    val server = Vertxes.vertx.createNetServer(netServerOptions)
    server.connectHandler { OBSSwapper(it, netClient, manager) }
    server.listen {
      if (it.succeeded()) {
        println("server start success!")
      }
    }
  }
}