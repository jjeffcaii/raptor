package me.zarafa.raptor.core.impl.ext

import me.zarafa.raptor.core.model.Chunk
import me.zarafa.raptor.core.model.FMT
import me.zarafa.raptor.core.model.Message
import me.zarafa.raptor.core.model.SimpleMessage
import me.zarafa.raptor.core.utils.Callback
import me.zarafa.raptor.core.utils.Codecs
import me.zarafa.raptor.core.utils.Filter
import com.google.common.base.Preconditions
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.concurrent.ForkJoinPool

class MessageFliper(private val filter: Filter<Chunk>? = null) {

  private var onMessage: Callback<Message>? = null
  private val queue = mutableListOf<Chunk>()
  private var size: Int = 0

  fun append(chunk: Chunk): MessageFliper {
    var pass = true
    this.filter?.let { pass = it.invoke(chunk) }
    if (!pass) {
      return this
    }
    this.queue.add(chunk)
    val msgLen = chunk.header.length
    val payloadLen = chunk.payload.length()
    size += payloadLen
    if (msgLen <= payloadLen || (chunk.header.fmt == FMT.F3 && msgLen == size)) {
      this.pop()
    }
    return this
  }

  fun onMessage(onMessage: Callback<Message>): MessageFliper {
    synchronized(this) {
      Preconditions.checkArgument(this.onMessage == null, "message handler exists already.")
      this.onMessage = onMessage
    }
    return this
  }

  private fun pop() {
    val first = queue.first()
    val payload = Buffer.buffer()
    queue.forEach {
      payload.appendBuffer(it.payload)
    }
    val b = Buffer.buffer()
    b.appendBuffer(first.basicHeader)
    first.messageHeader?.let { b.appendBuffer(it) }
    b.appendBuffer(payload)
    if (logger.isDebugEnabled) {
      logger.debug("<<< pop({} bytes): \n{}\n<<<", b.length(), Codecs.encodeHex(b.bytes, true))
    }

    this.onMessage?.let { msgHandler ->
      val simpleMessage = SimpleMessage(first.header, payload)
      ForkJoinPool.commonPool().run { msgHandler.invoke(simpleMessage) }
    }
    this.queue.clear()
    this.size = 0
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

}