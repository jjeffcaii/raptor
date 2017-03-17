package `as`.leap.raptor.core.model.msg

import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.msg.payload.*
import `as`.leap.raptor.core.utils.CodecHelper
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class Chunk(private val buffer: Buffer, private val header: Header) : Message<Payload> {

  override fun toBuffer(): Buffer {
    return this.buffer
  }

  override fun type(): MessageType {
    return MessageType.CHUNK
  }

  private fun calcPayloadOffset(): Int {
    var skip: Int
    if (this.header.csid < 64) {
      skip = 1
    } else if (this.header.csid < 320) {
      skip = 2
    } else {
      skip = 3
    }
    when (this.header.fmt) {
      FMT.F0 -> skip += 11
      FMT.F1 -> skip += 7
      FMT.F2 -> skip += 3
      else -> {
        // nothing to do.
      }
    }
    if (this.header.timestamp > 16777215) {
      skip += 4
    }
    return skip
  }

  private val model: Payload by lazy {
    val b = this.buffer.slice(this.calcPayloadOffset(), this.buffer.length())
    when (this.header.type) {
      ChunkType.CTRL_SET_CHUNK_SIZE -> {
        ProtocolChunkSize(b.getUnsignedInt(0))
      }
      ChunkType.CTRL_ABORT_MESSAGE -> {
        ProtocolAbortMessage(b.getUnsignedInt(0))
      }
      ChunkType.CTRL_SET_WINDOW_SIZE -> {
        ProtocolWindowSize(b.getUnsignedInt(0))
      }
      ChunkType.CTRL_ACK_WINDOW_SIZE -> {
        ProtocolAckWindowSize(b.getUnsignedInt(0))
      }
      ChunkType.CTRL_SET_PEER_BANDWIDTH -> {
        val limitType: Short = when {
          b.length() > 4 -> b.getUnsignedByte(4)
          else -> 0
        }
        ProtocolBandWidth(b.getUnsignedInt(0), limitType)
      }
      ChunkType.COMMAND_AMF0 -> {
        toCommand(CodecHelper.decodeAMF0(b.bytes), ChunkType.COMMAND_AMF0)
      }
      ChunkType.COMMAND_AMF3 -> {
        toCommand(CodecHelper.decodeAMF3(b.bytes), ChunkType.COMMAND_AMF3)
      }
      else -> {
        UnknownPayload(this.header.type)
      }
    }
  }

  private fun toCommand(values: List<Any>, type: ChunkType): Payload {
    val cmd = values[0] as String
    return when (cmd) {
      "_result" -> CommandResult(values, type)
      "_error" -> CommandError(values, type)
      "onStatus" -> CommandOnStatus(values, type)
      "releaseStream" -> CommandReleaseStream(values, type)
      "connect" -> CommandConnect(values, type)
      "FCPublish" -> CommandFCPublish(values, type)
      "onFCPublish" -> CommandOnFCPublish(values, type)
      "createStream" -> CommandCreateStream(values, type)
      "_checkbw" -> CommandCheckBW(values, type)
      "publish" -> CommandPublish(values, type)
      "deleteStream" -> CommandDeleteStream(values, type)
      "close" -> CommandClose(values, type)
      else -> {
        logger.warn("unknown command name: {}", cmd)
        UnknownPayload(type)
      }
    }
  }

  override fun toModel(): Payload {
    return this.model
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

}