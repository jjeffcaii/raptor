package `as`.leap.raptor.core.model.payload

import com.google.common.base.MoreObjects
import io.vertx.core.buffer.Buffer

class ProtocolChunkSize(val chunkSize: Long) : AbstractProtocol() {

  override fun toBuffer(): Buffer {
    return Buffer.buffer(4).appendUnsignedInt(this.chunkSize)
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("chunkSize", this.chunkSize)
        .toString()
  }

}