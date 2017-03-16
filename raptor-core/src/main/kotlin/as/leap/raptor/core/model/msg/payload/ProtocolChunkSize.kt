package `as`.leap.raptor.core.model.msg.payload

import `as`.leap.raptor.core.model.ChunkType
import com.google.common.base.MoreObjects

class ProtocolChunkSize(val chunkSize: Long) : Protocol() {

  override fun type(): ChunkType {
    return ChunkType.CTRL_SET_CHUNK_SIZE
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("chunkSize", this.chunkSize)
        .toString()
  }

}