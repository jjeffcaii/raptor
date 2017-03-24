package `as`.leap.raptor.core

import `as`.leap.raptor.core.utils.CodecHelper
import `as`.leap.raptor.core.utils.Vertxes
import io.vertx.kotlin.core.net.NetServerOptions

object PureProxyTest {

  @JvmStatic
  fun main(args: Array<String>) {

    val netServerOptions = NetServerOptions(tcpNoDelay = true, port = 1935)
    val server = Vertxes.vertx.createNetServer(netServerOptions)
    server.connectHandler { socket ->
      socket.pause()
      Vertxes.netClient.connect(1935, "pili-publish.maxwon.cn", {
        if (it.succeeded()) {
          val remote = it.result()
          remote
              .closeHandler { println("close2.") }
              .exceptionHandler { println("error2.") }
              .handler {
                println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<${it.length()} bytes")
                println(CodecHelper.encodeHex(it.bytes, true))
                println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<${it.length()} bytes")
                socket.write(it)
              }
          socket
              .handler {
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>${it.length()} bytes")
                println(CodecHelper.encodeHex(it.bytes, true))
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>${it.length()} bytes")
                remote.write(it)
              }
              .closeHandler { println("close.") }
              .exceptionHandler { println("error.") }
          socket.resume()
        }
      })
    }
    server.listen {
      if (it.succeeded()) {
        println("server start success!")
      }
    }

  }

}