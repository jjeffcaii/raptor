package `as`.leap.raptor.core.model.payload

import com.google.common.base.MoreObjects
import io.vertx.core.buffer.Buffer

class ProtocolAbortMessage(val csid: Long) : AbstractProtocol() {

  override fun toBuffer(): Buffer {
    return Buffer.buffer(4).appendUnsignedInt(this.csid)
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("csid", this.csid)
        .toString()
  }

}