package `as`.leap.raptor.core.model.msg.payload

import `as`.leap.raptor.core.model.ChunkType
import com.google.common.base.MoreObjects

class ProtocolWindowSize(val sequence: Long) : Protocol() {

  override fun type(): ChunkType {
    return ChunkType.CTRL_SET_WINDOW_SIZE
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("sequence", this.sequence)
        .toString()
  }

}