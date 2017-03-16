package `as`.leap.raptor.core.model

data class Header(
    val fmt: FMT,
    val csid: Int,
    val timestamp: Long = 0,
    val streamId: Long,
    val type: ChunkType,
    val length: Int?
)