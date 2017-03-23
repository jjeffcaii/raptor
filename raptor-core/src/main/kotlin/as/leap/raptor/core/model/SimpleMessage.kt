package `as`.leap.raptor.core.model

import `as`.leap.raptor.core.model.payload.*
import `as`.leap.raptor.core.utils.CodecHelper
import com.google.common.base.Preconditions
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles


class SimpleMessage(header: Header, private val payload: Buffer) : Message(header) {

  override fun toChunks(chunkSize: Int): Array<Chunk> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun toModel(): Payload {
    val type = this.header.type
    return when (type) {
      ChunkType.CTRL_SET_CHUNK_SIZE -> ProtocolChunkSize(this.payload.getUnsignedInt(0))
      ChunkType.CTRL_ABORT_MESSAGE -> ProtocolAbortMessage(this.payload.getUnsignedInt(0))
      ChunkType.CTRL_SET_WINDOW_SIZE -> ProtocolWindowSize(this.payload.getUnsignedInt(0))
      ChunkType.CTRL_ACK_WINDOW_SIZE -> ProtocolAckWindowSize(this.payload.getUnsignedInt(0))
      ChunkType.CTRL_SET_PEER_BANDWIDTH -> toProtocolBandWidth(this.payload)
      ChunkType.COMMAND_AMF0 -> toCommand(CodecHelper.decodeAMF0(this.payload.bytes), type)
      ChunkType.COMMAND_AMF3 -> toCommand(CodecHelper.decodeAMF3(this.payload.bytes), type)
      ChunkType.DATA_AMF0 -> SimpleAMFPayload(CodecHelper.decodeAMF0(this.payload.bytes), type)
      ChunkType.DATA_AMF3 -> SimpleAMFPayload(CodecHelper.decodeAMF0(this.payload.bytes), type)
/*
      ChunkType.USER_CONTROL -> TODO()
      ChunkType.SHARE_OBJECT_AMF0 -> TODO()
      ChunkType.SHARE_OBJECT_AMF3 -> TODO()
      ChunkType.MEDIA_AUDIO -> TODO()
      ChunkType.MEDIA_VIDEO -> TODO()
      ChunkType.AGGREGATE -> TODO()
*/
      else -> SimpleBinaryPayload(this.payload.bytes, type)
    }
  }


  override fun toBuffer(): Buffer {
    val headerCopy = Header(this.header.fmt, this.header.csid, this.header.timestamp, this.header.streamId, this.header.type, this.payload.length())
    return Buffer.buffer().appendBuffer(headerCopy.toBuffer()).appendBuffer(this.payload)
  }

  companion object {

    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private fun toCommand(values: Array<Any?>, type: ChunkType): Payload {
      Preconditions.checkArgument(values.size > 2, "Not valid AMF objects length: ${values.size}")
      Preconditions.checkArgument(values[0] is String, "Not valid cmd type: ${values[0]!!::class}")
      Preconditions.checkArgument(values[1] is Number, "Not valid transId type: ${values[1]!!::class}")
      val first = values[0] as String
      val second = (values[1] as Number).toInt()
      val others = values.slice(2 until values.size).toTypedArray()
      return when (first) {
        CommandResult.NAME -> CommandResult(second, others)
        CommandError.NAME -> CommandError(second, others)
        CommandOnStatus.NAME -> CommandOnStatus(second, others)
        CommandReleaseStream.NAME -> CommandReleaseStream(second, others)
        CommandConnect.NAME -> CommandConnect(second, others)
        CommandFCPublish.NAME -> CommandFCPublish(second, others)
        CommandOnFCPublish.NAME -> CommandOnFCPublish(second, others)
        CommandCreateStream.NAME -> CommandCreateStream(second, others)
        CommandCheckBW.NAME -> CommandCheckBW(second, others)
        CommandPublish.NAME -> CommandPublish(second, others)
        CommandDeleteStream.NAME -> CommandDeleteStream(second, others)
        CommandClose.NAME -> CommandClose(second, others)
        else -> {
          logger.warn("unsupported command name: {}", first)
          SimpleAMFPayload(values, type)
        }
      }
    }

    private fun toProtocolBandWidth(payload: Buffer): ProtocolBandWidth {
      return ProtocolBandWidth(payload.getUnsignedInt(0), if (payload.length() > 4) {
        payload.getUnsignedByte(4)
      } else {
        0
      })
    }

  }

}