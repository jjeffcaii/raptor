package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.FMT
import `as`.leap.raptor.core.utils.CodecHelper
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles


class ChunkAggregator {

  private val queue = mutableListOf<Chunk>()
  private var size: Int = 0

  fun push(chunk: Chunk): ChunkAggregator {
    this.queue.add(chunk)
    val msgLen = chunk.header.length!!
    val payloadLen = chunk.payload.length()
    size += payloadLen
    if (msgLen <= payloadLen || (chunk.header.fmt == FMT.F3 && msgLen == size)) {
      this.pop()
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
    this.queue.clear()
    this.size = 0
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

}