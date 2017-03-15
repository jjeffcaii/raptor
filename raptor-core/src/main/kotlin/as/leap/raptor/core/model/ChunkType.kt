package `as`.leap.raptor.core.model

enum class ChunkType(val code: Byte) {
  CTRL_SET_CHUNK_SIZE(0x01),
  CTRL_ABORT_MESSAGE(0x02),
  CTRL_SET_WINDOW_SIZE(0x03),
  USER_CONTROL(0x04),
  CTRL_ACK_WINDOW_SIZE(0x05),
  CTRL_SET_PEER_BANDWIDTH(0x06),
  COMMAND_AMF0(0x14),
  COMMAND_AMF3(0x11),
  DATA_AMF0(0x12),
  DATA_AMF3(0x0f),
  SHARE_OBJECT_AMF0(0x13),
  SHARE_OBJECT_AMF3(0x10),
  MEDIA_AUDIO(0x08),
  MEDIA_VIDEO(0x09),
  AGGREGATE(0x16);

  companion object {
    private val dic: Map<Byte, ChunkType> by lazy {
      ChunkType.values().map { t -> t.code to t }.toMap()
    }

    fun toChunkType(code: Byte): ChunkType? {
      return dic[code]
    }
  }
}