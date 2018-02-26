package me.zarafa.raptor.server

import io.vertx.core.Vertx
import io.vertx.core.net.NetServer
import io.vertx.kotlin.core.net.NetServerOptions
import me.zarafa.raptor.api.ChannelManager
import me.zarafa.raptor.core.Swapper
import me.zarafa.raptor.core.impl.DefaultSwapper
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class RTMPProxyServer(
    private val channelManager: ChannelManager,
    private val port: Int = 1935,
    private val retry: Int = 3,
    private val strategy: Swapper.LiveStrategy = Swapper.LiveStrategy.ALL
) : Runnable {
  private val vertx = Vertx.vertx()
  private val rtmpServer: NetServer

  init {
    // create rtmp server.
    this.rtmpServer = vertx.createNetServer(NetServerOptions(tcpNoDelay = true, usePooledBuffers = true))
    val netClient = vertx.createNetClient()
    this.rtmpServer.connectHandler {
      it.pause()
      DefaultSwapper(it, netClient, this.strategy, this.retry, channelManager)
      it.resume()
    }
  }

  override fun run() {
    this.rtmpServer.listen(this.port, {
      when (it.succeeded()) {
        true -> logger.info("RTMP proxy server is listen: port={}.", this.port)
        else -> {
          logger.error("RTMP server start failed!!!", it.cause())
          System.exit(2)
        }
      }
    })
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }
}