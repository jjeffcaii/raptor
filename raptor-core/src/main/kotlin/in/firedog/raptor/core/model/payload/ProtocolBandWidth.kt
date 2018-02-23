package `in`.firedog.raptor.core.model.payload

import com.google.common.base.MoreObjects
import io.vertx.core.buffer.Buffer

class ProtocolBandWidth(val bandWidth: Long = 2500000, val limit: Short = 0) : AbstractProtocol() {

  override fun toBuffer(): Buffer {
    return if (limit > 0) {
      Buffer.buffer(4).appendUnsignedInt(this.bandWidth).appendUnsignedByte(this.limit)
    } else {
      Buffer.buffer(4).appendUnsignedInt(this.bandWidth)
    }
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("bandWidth", this.bandWidth)
        .add("limit", this.limit)
        .toString()
  }

}