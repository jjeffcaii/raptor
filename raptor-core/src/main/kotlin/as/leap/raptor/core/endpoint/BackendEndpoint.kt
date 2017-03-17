package `as`.leap.raptor.core.endpoint

import `as`.leap.raptor.core.Endpoint
import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.utils.VertxHelper
import com.google.common.collect.Queues
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.concurrent.BlockingQueue

class BackendEndpoint(
    private val host: String,
    private val port: Int = 1935,
    private val consumer: (Message<Any>) -> Unit,
    private val onError: ((Throwable) -> Unit)?) : Endpoint(consumer) {

  private val queue: BlockingQueue<Message<Any>> = Queues.newLinkedBlockingQueue()
  private var socket: NetSocket? = null

  init {
    VertxHelper.netClient.connect(this.port, this.host, {
      if (it.succeeded()) {
        logger.info("create backend socket success.")
        this.socket = it.result()
      } else {
        logger.error("create backend socket failed.", it.cause())
        this.onError?.invoke(it.cause())
      }
    })
  }

  override fun write(buffer: Buffer): Endpoint {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }
}