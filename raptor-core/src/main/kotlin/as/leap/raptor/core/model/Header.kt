package `as`.leap.raptor.core.model

import com.google.common.base.MoreObjects
import io.vertx.core.buffer.Buffer

data class Header(
    var fmt: FMT,
    var csid: Int,
    var timestamp: Long = 0,
    var streamId: Long = 0L,
    var type: ChunkType,
    var length: Int = 0
) : Buffered {

  override fun toBuffer(): Buffer {
    val b = Buffer.buffer()
    if (this.csid < 64) {
      val v = this.fmt.code shl 6 or this.csid
      b.appendByte(v.toByte())
    } else if (this.csid < 320) {
      val v = this.fmt.code shl 6
      b.appendByte(v.toByte())
      b.appendByte((this.csid - 64).toByte())
    } else {
      val v = this.fmt.code shl 6 or 1
      b.appendByte(v.toByte())
      b.appendUnsignedShortLE(this.csid - 64)
    }

    val hasExtendedTimestamp = this.timestamp > 0xFFFFFF
    val ts: Int = if (hasExtendedTimestamp) 0x7FFFFF else this.timestamp.toInt()
    when (this.fmt) {
      FMT.F0 -> {
        b.appendMedium(ts)
        b.appendMedium(this.length)
        b.appendByte(this.type.code)
        b.appendUnsignedIntLE(this.streamId)
      }
      FMT.F1 -> {
        b.appendMedium(ts)
        b.appendMedium(this.length)
        b.appendByte(this.type.code)
      }
      FMT.F2 -> {
        b.appendMedium(ts)
      }
      else -> {
        // ignore
      }
    }
    if (hasExtendedTimestamp) {
      b.appendUnsignedInt(this.timestamp)
    }
    return b
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("fmt", this.fmt)
        .add("csid", this.csid)
        .add("timestamp", this.timestamp)
        .add("streamId", this.streamId)
        .add("type", this.type)
        .add("length", this.length)
        .omitNullValues()
        .toString()
  }

  companion object {

    fun getProtocolHeader(type: ChunkType, len: Int = 4): Header {
      return Header(FMT.F0, 2, 0L, 0L, type, len)
    }
  }

}