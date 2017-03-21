package `as`.leap.raptor.core.model.msg.payload

import com.google.common.base.MoreObjects

class ProtocolChunkSize(val chunkSize: Long) : Protocol() {

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("chunkSize", this.chunkSize)
        .toString()
  }

}