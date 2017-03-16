package `as`.leap.raptor.core.model.msg.payload

import `as`.leap.raptor.core.model.ChunkType
import com.google.common.base.MoreObjects

class ProtocolAckWindowSize(val windowSize: Long) : Protocol() {
  override fun type(): ChunkType {
    return ChunkType.CTRL_ACK_WINDOW_SIZE
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("windowSize", windowSize)
        .toString()
  }
}