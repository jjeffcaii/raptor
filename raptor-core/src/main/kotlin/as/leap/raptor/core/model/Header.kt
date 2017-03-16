package `as`.leap.raptor.core.model

import com.google.common.base.MoreObjects

data class Header(
    val fmt: FMT,
    val csid: Int,
    val timestamp: Long = 0,
    val streamId: Long,
    val type: ChunkType,
    val length: Int?
) {
  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("fmt", this.fmt)
        .add("csid", this.csid)
        .add("timestamp", this.timestamp)
        .add("streamId", this.streamId)
        .add("type", this.type)
        .add("length", this.length)
        .omitNullValues()
        .toString()
  }
}