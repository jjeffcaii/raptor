package `as`.leap.raptor.core.utils

import io.vertx.core.Vertx
import io.vertx.core.net.NetClient
import io.vertx.kotlin.core.net.NetClientOptions


object Vertxes {

  val vertx: Vertx by lazy {
    Vertx.vertx()
  }

  val netClient: NetClient by lazy {
    val opts = NetClientOptions(connectTimeout = 15000, tcpNoDelay = true, usePooledBuffers = true)
    this.vertx.createNetClient(opts)
  }

}