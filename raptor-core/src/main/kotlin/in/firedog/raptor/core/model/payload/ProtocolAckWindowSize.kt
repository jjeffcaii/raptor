package `in`.firedog.raptor.core.model.payload

import com.google.common.base.MoreObjects
import io.vertx.core.buffer.Buffer

class ProtocolAckWindowSize(val windowSize: Long = 2500000) : AbstractProtocol() {
  override fun toBuffer(): Buffer {
    return Buffer.buffer(4).appendUnsignedInt(this.windowSize)
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("windowSize", windowSize)
        .toString()
  }
}