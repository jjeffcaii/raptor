package me.zarafa.raptor.core.model.payload

import com.google.common.base.MoreObjects
import io.vertx.core.buffer.Buffer

class ProtocolWindowSize(val sequence: Long = 2500000) : AbstractProtocol() {

  override fun toBuffer(): Buffer {
    return Buffer.buffer(4).appendUnsignedInt(this.sequence)
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("sequence", this.sequence)
        .toString()
  }

}