package `as`.leap.raptor.core

import `as`.leap.raptor.core.utils.X
import io.vertx.core.buffer.Buffer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class SimpleEndpoint(private val host: String, private val port: Int) : Endpoint {

  private val queue: BlockingQueue<Buffer> = LinkedBlockingQueue()

  init {
    X.netClient.connect(port, host, { event ->

    })
  }

  override fun write(buffer: Buffer): Endpoint {
    this.queue.add(buffer)
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}