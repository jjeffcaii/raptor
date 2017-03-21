package `as`.leap.raptor.core.model

import io.vertx.core.buffer.Buffer

data class Chunk(val header: Header, val basicHeader: Buffer, val messageHeader: Buffer? = null, val payload: Buffer) : Buffered {

  override fun toBuffer(): Buffer {
    val b = Buffer.buffer()
    b.appendBuffer(this.basicHeader)
    messageHeader?.let { b.appendBuffer(it) }
    b.appendBuffer(this.payload)
    return b
  }

}