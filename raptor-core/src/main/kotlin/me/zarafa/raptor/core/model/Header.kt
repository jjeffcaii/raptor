package me.zarafa.raptor.core.model

import com.google.common.base.MoreObjects
import io.vertx.core.buffer.Buffer
import me.zarafa.raptor.core.utils.Buffered

data class Header(
    var fmt: FMT,
    var csid: Int,
    var type: MessageType,
    var length: Int = 0,
    var timestamp: Long = 0,
    var streamId: Long = 0L
) : Buffered {

  fun toBasicHeader(): Buffer {
    val b = Buffer.buffer()
    when {
      this.csid < 64 -> {
        val v = this.fmt.code shl 6 or this.csid
        b.appendByte(v.toByte())
      }
      this.csid < 320 -> {
        val v = this.fmt.code shl 6
        b.appendByte(v.toByte())
        b.appendByte((this.csid - 64).toByte())
      }
      else -> {
        val v = this.fmt.code shl 6 or 1
        b.appendByte(v.toByte())
        b.appendUnsignedShortLE(this.csid - 64)
      }
    }
    return b
  }

  fun toMsgHeader(): Buffer {
    val b = Buffer.buffer()
    val ets = this.hasExtendedTimestamp()
    val ts: Int = if (ets) 0x7FFFFF else this.timestamp.toInt()
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
    return b
  }

  private fun hasExtendedTimestamp(): Boolean {
    return this.timestamp > 0xFFFFFF
  }

  override fun toBuffer(): Buffer {
    val buffer = Buffer.buffer()
        .appendBuffer(this.toBasicHeader())
        .appendBuffer(this.toMsgHeader())
    if (this.hasExtendedTimestamp()) {
      buffer.appendUnsignedInt(this.timestamp)
    }
    return buffer
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

    fun getProtocolHeader(type: MessageType, len: Int = 4): Header {
      return Header(FMT.F0, 2, type, len)
    }

    fun clone(header: Header): Header {
      return Header(header.fmt, header.csid, header.type, header.length, header.timestamp, header.streamId)
    }

  }

}