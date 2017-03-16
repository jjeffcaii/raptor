package `as`.leap.raptor.core

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.net.NetSocket
import io.vertx.core.parsetools.RecordParser
import io.vertx.kotlin.core.net.NetClientOptions
import io.vertx.kotlin.core.net.NetServerOptions
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.concurrent.LinkedBlockingQueue


object SimpleNetServer {

  private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())!!
  private val clientOptions = NetClientOptions(connectTimeout = 5000)
  private val serverOptions = NetServerOptions(port = 1935)

  private fun initSocket(socket: NetSocket) {
    socket.closeHandler {
      logger.info("socket closed.")
    }
    socket.exceptionHandler { throwable ->
      logger.error("socket error.", throwable)
    }
  }

  private val vertx = Vertx.vertx()
  private val server = vertx.createNetServer(serverOptions)
  private val client = vertx.createNetClient(this.clientOptions)

  fun conn(): Future<NetSocket> {
    return Future.future<NetSocket> { handler ->
      client.connect(1935, "pili-live-rtmp.maxwon.cn", { event ->
        if (event.succeeded()) {
          handler.complete(event.result())

        } else {
          handler.fail(event.cause())
        }
      })
    }
  }


  @JvmStatic
  fun main(args: Array<String>) {

    server.connectHandler({ front ->
      this.initSocket(front)

      val q = LinkedBlockingQueue<NetSocket>(1)

      val parser = RecordParser.newFixed(1, null)
      val handler = MessageFliper(parser, { msg ->
        logger.info("got {} message: {} bytes.", msg.type().name, msg.buffer().length())
        val backend = q.take()

        backend.write(msg.buffer())
        q.add(backend)
      })
      parser.setOutput(handler)
      front.handler(parser)

      this.conn().setHandler { h ->
        val backend = h.result()
        this.initSocket(backend)
        backend.handler { buffer -> front.write(buffer) }
        q.add(backend)
      }
    })

    server.listen({ result ->
      if (result.succeeded()) {
        logger.info("server start success!")
      }
    })
  }
}