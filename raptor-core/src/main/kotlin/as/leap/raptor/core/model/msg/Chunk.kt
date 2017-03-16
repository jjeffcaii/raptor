package `as`.leap.raptor.core.model.msg

import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.msg.payload.*
import `as`.leap.raptor.core.utils.CodecHelper
import io.vertx.core.buffer.Buffer

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
        val arr = CodecHelper.decodeAMF0(b.bytes)
        when (arr[0] as String) {
          "connect" -> {
            CommandConnect(ChunkType.COMMAND_AMF0, (arr[1] as Number).toInt(), arr[2])
          }
          else -> EmptyPayload.INSTANCE
        }
      }
      else -> {
        EmptyPayload.INSTANCE
      }
    }
  }

  override fun toModel(): Payload {
    return this.model
  }

}