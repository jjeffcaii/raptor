package `as`.leap.raptor.core.model.msg.payload

import com.google.common.base.MoreObjects

class ProtocolBandWidth(val bw: Long, val limit: Short = 0) : Protocol() {

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("bw", this.bw)
        .add("limit", this.limit)
        .toString()
  }

}