package `as`.leap.raptor.core.model.msg.payload

import com.google.common.base.MoreObjects

class ProtocolAckWindowSize(val windowSize: Long) : Protocol() {

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("windowSize", windowSize)
        .toString()
  }
}