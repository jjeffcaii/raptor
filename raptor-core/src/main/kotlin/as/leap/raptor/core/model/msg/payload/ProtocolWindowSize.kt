package `as`.leap.raptor.core.model.msg.payload

import com.google.common.base.MoreObjects

class ProtocolWindowSize(val sequence: Long = 2500000) : Protocol() {

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("sequence", this.sequence)
        .toString()
  }

}