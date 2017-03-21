package `as`.leap.raptor.core.model

import `as`.leap.raptor.core.model.msg.Payload
import `as`.leap.raptor.core.model.msg.payload.*
import `as`.leap.raptor.core.utils.CodecHelper
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles


class SimpleMessage(header: Header, private val payload: Buffer) : Message(header) {

  override fun toChunks(chunkSize: Int): Array<Chunk> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun toModel(): Payload {
    return when (this.header.type) {
      ChunkType.CTRL_SET_CHUNK_SIZE -> ProtocolChunkSize(this.payload.getUnsignedInt(0))
      ChunkType.CTRL_ABORT_MESSAGE -> ProtocolAbortMessage(this.payload.getUnsignedInt(0))
      ChunkType.CTRL_SET_WINDOW_SIZE -> ProtocolWindowSize(this.payload.getUnsignedInt(0))
      ChunkType.CTRL_ACK_WINDOW_SIZE -> ProtocolAckWindowSize(this.payload.getUnsignedInt(0))
      ChunkType.CTRL_SET_PEER_BANDWIDTH -> {
        val bandWidth = this.payload.getUnsignedInt(0)
        val limitType = when {
          this.payload.length() > 4 -> this.payload.getUnsignedByte(4)
          else -> 0
        }
        ProtocolBandWidth(bandWidth, limitType)

      }
      ChunkType.COMMAND_AMF0 -> toCommand(CodecHelper.decodeAMF0(this.payload.bytes), this.header.type)
      ChunkType.COMMAND_AMF3 -> toCommand(CodecHelper.decodeAMF3(this.payload.bytes), this.header.type)
/*
      ChunkType.USER_CONTROL -> TODO()
      ChunkType.DATA_AMF0 -> TODO()
      ChunkType.DATA_AMF3 -> TODO()
      ChunkType.SHARE_OBJECT_AMF0 -> TODO()
      ChunkType.SHARE_OBJECT_AMF3 -> TODO()
      ChunkType.MEDIA_AUDIO -> TODO()
      ChunkType.MEDIA_VIDEO -> TODO()
      ChunkType.AGGREGATE -> TODO()
*/
      else -> UnknownPayload(this.header.type)
    }
  }

  override fun toBuffer(): Buffer {
    val b = Buffer.buffer()
    if (this.header.csid < 64) {
      val v = this.header.fmt.code shl 6 or this.header.csid
      b.appendByte(v.toByte())
    } else if (this.header.csid < 320) {
      val v = this.header.fmt.code shl 6
      b.appendByte(v.toByte())
      b.appendByte((this.header.csid - 64).toByte())
    } else {
      val v = this.header.fmt.code shl 6 or 1
      b.appendByte(v.toByte())
      b.appendUnsignedShortLE(this.header.csid - 64)
    }

    val hasExtendedTimestamp = this.header.timestamp > 0xFFFFFF
    val ts: Int = if (hasExtendedTimestamp) 0x7FFFFF else this.header.timestamp.toInt()

    when (this.header.fmt) {
      FMT.F0 -> {
        b.appendMedium(ts)
        b.appendMedium(payload.length())
        b.appendByte(this.header.type.code)
        b.appendUnsignedIntLE(this.header.streamId)
      }
      FMT.F1 -> {
        b.appendMedium(ts)
        b.appendMedium(payload.length())
        b.appendByte(this.header.type.code)
      }
      FMT.F2 -> {
        b.appendMedium(ts)
      }
      else -> {
        // do nothing
      }
    }
    if (hasExtendedTimestamp) {
      b.appendUnsignedInt(this.header.timestamp)
    }
    b.appendBuffer(this.payload)
    return b
  }

  private fun toCommand(values: List<Any>, type: ChunkType): Payload {
    val cmd = values[0] as String
    return when (cmd) {
      "_result" -> CommandResult(values)
      "_error" -> CommandError(values)
      "onStatus" -> CommandOnStatus(values)
      "releaseStream" -> CommandReleaseStream(values)
      "connect" -> CommandConnect(values)
      "FCPublish" -> CommandFCPublish(values)
      "onFCPublish" -> CommandOnFCPublish(values)
      "createStream" -> CommandCreateStream(values)
      "_checkbw" -> CommandCheckBW(values)
      "publish" -> CommandPublish(values)
      "deleteStream" -> CommandDeleteStream(values)
      "close" -> CommandClose(values)
      else -> {
        logger.warn("unknown command name: {}", cmd)
        UnknownPayload(type)
      }
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }


}