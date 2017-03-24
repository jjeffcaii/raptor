package `as`.leap.raptor.core.model

import `as`.leap.raptor.core.utils.Buffered
import `as`.leap.raptor.core.utils.CodecHelper
import com.google.common.base.MoreObjects
import com.google.common.hash.Hashing
import io.vertx.core.buffer.Buffer

data class Handshake(val buffer: Buffer) : Buffered {

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

  override fun toBuffer(): Buffer {
    return this.buffer
  }

  class C0(val version: Short = 3) : Buffered {

    override fun toBuffer(): Buffer {
      val b = Buffer.buffer(1)
      b.appendUnsignedByte(this.version)
      return b
    }

    override fun toString(): String {
      return MoreObjects.toStringHelper(this)
          .add("version", this.version)
          .toString()
    }

  }

  class C12(val v1: Long, val v2: Long, val random: Buffer) : Buffered {

    override fun toBuffer(): Buffer {
      return Buffer.buffer(1536)
          .appendUnsignedInt(this.v1)
          .appendUnsignedInt(this.v2)
          .appendBuffer(this.random)
    }

    fun hash(): Triple<Long, Long, String> {
      val third = Hashing.murmur3_32().newHasher()
          .putBytes(this.random.bytes)
          .hash()
          .toString()
      return Triple(this.v1, this.v2, third)
    }

    override fun toString(): String {
      return MoreObjects.toStringHelper(this)
          .add("v1", this.v1)
          .add("v2", this.v2)
          .add("random", "MD5(${CodecHelper.md5(this.random.bytes)})")
          .toString()
    }

  }

  companion object {
    val C0_INSTANCE = C0()
  }

}