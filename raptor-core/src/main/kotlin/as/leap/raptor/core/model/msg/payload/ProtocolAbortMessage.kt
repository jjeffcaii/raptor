package `as`.leap.raptor.core.model.msg.payload

import `as`.leap.raptor.core.model.ChunkType
import com.google.common.base.MoreObjects

class ProtocolAbortMessage(val csid: Long) : Protocol() {
  override fun type(): ChunkType {
    return ChunkType.CTRL_ABORT_MESSAGE
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("csid", this.csid)
        .toString()
  }

}