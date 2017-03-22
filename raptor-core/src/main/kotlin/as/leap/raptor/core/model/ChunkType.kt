package `as`.leap.raptor.core.model

enum class ChunkType(val code: Byte) {
  // protocol control
  CTRL_SET_CHUNK_SIZE(0x01),
  CTRL_ABORT_MESSAGE(0x02),
  CTRL_SET_WINDOW_SIZE(0x03),
  CTRL_ACK_WINDOW_SIZE(0x05),
  CTRL_SET_PEER_BANDWIDTH(0x06),
  // user control event
  USER_CONTROL(0x04),
  // commands
  COMMAND_AMF0(0x14),
  COMMAND_AMF3(0x11),
  // data
  DATA_AMF0(0x12),
  DATA_AMF3(0x0f),
  MEDIA_AUDIO(0x08),
  MEDIA_VIDEO(0x09),
  // others
  SHARE_OBJECT_AMF0(0x13),
  SHARE_OBJECT_AMF3(0x10),
  AGGREGATE(0x16);

  fun isProtocol(): Boolean {
    return when (this) {
      CTRL_SET_CHUNK_SIZE, CTRL_SET_WINDOW_SIZE, CTRL_ABORT_MESSAGE, CTRL_ACK_WINDOW_SIZE, CTRL_SET_PEER_BANDWIDTH -> true
      else -> false
    }
  }

  fun isData(): Boolean {
    return when (this) {
      DATA_AMF0, DATA_AMF3, MEDIA_AUDIO, MEDIA_VIDEO -> true
      else -> false
    }
  }

  fun isCommand(): Boolean {
    return when (this) {
      COMMAND_AMF0, COMMAND_AMF3 -> true
      else -> false
    }
  }

  companion object {
    fun toChunkType(code: Short): ChunkType? {
      return when (code.toInt()) {
        1 -> CTRL_SET_CHUNK_SIZE
        2 -> CTRL_ABORT_MESSAGE
        3 -> CTRL_SET_WINDOW_SIZE
        4 -> USER_CONTROL
        5 -> CTRL_ACK_WINDOW_SIZE
        6 -> CTRL_SET_PEER_BANDWIDTH
        20 -> COMMAND_AMF0
        17 -> COMMAND_AMF3
        18 -> DATA_AMF0
        15 -> DATA_AMF3
        19 -> SHARE_OBJECT_AMF0
        16 -> SHARE_OBJECT_AMF3
        8 -> MEDIA_AUDIO
        9 -> MEDIA_VIDEO
        22 -> AGGREGATE
        else -> throw UnsupportedOperationException("Not valid chunk type: byte=$code.")
      }
    }
  }
}