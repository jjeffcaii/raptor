package `as`.leap.raptor.core.model.msg.payload

import com.google.common.base.MoreObjects

class ProtocolAbortMessage(val csid: Long) : Protocol() {

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("csid", this.csid)
        .toString()
  }

}