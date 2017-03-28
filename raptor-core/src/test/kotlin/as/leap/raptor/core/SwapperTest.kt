package `as`.leap.raptor.core

import `as`.leap.raptor.api.SecurityManager
import `as`.leap.raptor.core.impl.DefaultSwapper
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
    val server = vertx.createNetServer(netServerOptions)
    server.connectHandler {
      it.pause()
      DefaultSwapper(it, netClient, manager, object : SecurityManager {
        override fun exists(namespace: String): Boolean {
          TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun validate(namespace: String, streamKey: String): SecurityManager.Result {
          TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
      })
      it.resume()
    }
    server.listen {
      if (it.succeeded()) {
        println("server start success!")
      }
    }
  }
}