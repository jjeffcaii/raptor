package `as`.leap.raptor.core.model

import com.google.common.hash.Hashing
import io.vertx.core.buffer.Buffer

data class Handshake(val buffer: Buffer) {

  fun toModel(): Any {
    return when (buffer.length()) {
      1 -> C0(buffer.getUnsignedByte(0))
      1536 -> {
        val v1 = buffer.getUnsignedInt(0)
        val v2 = buffer.getUnsignedInt(4)
        val random = buffer.slice(8, 1536)
        C12(v1, v2, random)
      }
      else -> throw UnsupportedOperationException("Not valid handshake size: ${buffer.length()} bytes.")
    }
  }

  fun toBuffer(): Buffer {
    return this.buffer
  }

  class C0(val version: Short = 3) : Buffered {

    override fun toBuffer(): Buffer {
      val b = Buffer.buffer(1)
      b.appendUnsignedByte(this.version)
      return b
    }

  }

  class C12(val v1: Long, val v2: Long, val random: Buffer) : Buffered {

    override fun toBuffer(): Buffer {
      return Buffer.buffer(1536)
          .appendUnsignedInt(this.v1)
          .appendUnsignedInt(this.v2)
          .appendBuffer(this.random)
    }

    fun hash(): Int {
      return Hashing.murmur3_32().newHasher()
          .putLong(this.v1)
          .putLong(this.v2)
          .putBytes(this.random.bytes)
          .hash()
          .asInt()
    }

  }

}