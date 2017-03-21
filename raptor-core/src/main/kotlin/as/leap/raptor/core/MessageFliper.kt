package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.Chunk
import `as`.leap.raptor.core.model.FMT
import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.SimpleMessage
import `as`.leap.raptor.core.utils.Callback
import `as`.leap.raptor.core.utils.CodecHelper
import `as`.leap.raptor.core.utils.Filter
import com.google.common.base.Preconditions
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class MessageFliper(private val filter: Filter<Chunk>? = null) {

  private var onMessage: Callback<Message>? = null
  private val queue = mutableListOf<Chunk>()
  private var size: Int = 0

  fun append(chunk: Chunk): MessageFliper {
    var pass: Boolean = true
    this.filter?.let { pass = it.invoke(chunk) }
    if (!pass) {
      return this
    }
    this.queue.add(chunk)
    val msgLen = chunk.header.length!!
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
      logger.debug("<<< pop({} bytes): \n{}\n<<<", b.length(), CodecHelper.encodeHex(b.bytes, true))
    }
    this.onMessage?.invoke(SimpleMessage(first.header, payload))
    this.queue.clear()
    this.size = 0
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

}