package `as`.leap.raptor.core.model

data class Header(
    val fmt: FMT,
    val csid: Short,
    val timestamp: Long = 0,
    val streamId: Int,
    val type: MessageType,
    val length: Int?
)