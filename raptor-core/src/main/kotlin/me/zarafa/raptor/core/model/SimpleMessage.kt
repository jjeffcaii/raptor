package me.zarafa.raptor.core.model

import com.google.common.base.Preconditions
import io.vertx.core.buffer.Buffer
import me.zarafa.raptor.core.model.payload.*
import me.zarafa.raptor.core.utils.Codecs
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class SimpleMessage(header: Header, private val payload: Buffer) : Message(header) {

  override fun toChunks(chunkSize: Long): List<Chunk> {
    return this.toChunks(chunkSize.toInt())
  }

  override fun toBuffer(chunkSize: Long): Buffer {
    return this.toBuffer(chunkSize.toInt())
  }

  private fun toBuffer(chunkSize: Int): Buffer {
    return if (this.payload.length() > chunkSize) {
      val b = Buffer.buffer()
      this.toChunks(chunkSize).map(Chunk::toBuffer).forEach { b.appendBuffer(it) }
      b
    } else {
      this.toBuffer()
    }
  }

  private fun toChunks(chunkSize: Int): List<Chunk> {
    val li = mutableListOf<Chunk>()
    val totals = payload.length()
    var offset = 0
    do {
      val h = Header.clone(this.header)
      if (offset > 0) {
        h.fmt = FMT.F3
      }
      val sub = payload.slice(offset, Math.min(offset + chunkSize, totals))
      h.length = totals
      li.add(Chunk(h, h.toBasicHeader(), h.toMsgHeader(), sub))
      offset += chunkSize
    } while (offset < totals)
    return li
  }

  override fun toModel(): Payload {
    val type = this.header.type
    return when (type) {
      MessageType.CTRL_SET_CHUNK_SIZE -> ProtocolChunkSize(this.payload.getUnsignedInt(0))
      MessageType.CTRL_ABORT_MESSAGE -> ProtocolAbortMessage(this.payload.getUnsignedInt(0))
      MessageType.CTRL_SET_WINDOW_SIZE -> ProtocolWindowSize(this.payload.getUnsignedInt(0))
      MessageType.CTRL_ACK_WINDOW_SIZE -> ProtocolAckWindowSize(this.payload.getUnsignedInt(0))
      MessageType.CTRL_SET_PEER_BANDWIDTH -> toProtocolBandWidth(this.payload)
      MessageType.COMMAND_AMF0 -> toCommand(Codecs.decodeAMF0(this.payload.bytes), type)
      MessageType.COMMAND_AMF3 -> toCommand(Codecs.decodeAMF3(this.payload.bytes), type)
      MessageType.DATA_AMF0 -> SimpleAMFPayload(Codecs.decodeAMF0(this.payload.bytes), type)
      MessageType.DATA_AMF3 -> SimpleAMFPayload(Codecs.decodeAMF0(this.payload.bytes), type)
/*
      MessageType.USER_CONTROL -> TODO()
      MessageType.SHARE_OBJECT_AMF0 -> TODO()
      MessageType.SHARE_OBJECT_AMF3 -> TODO()
      MessageType.MEDIA_AUDIO -> TODO()
      MessageType.MEDIA_VIDEO -> TODO()
      MessageType.AGGREGATE -> TODO()
*/
      else -> SimpleBinaryPayload(this.payload.bytes, type)
    }
  }

  override fun toBuffer(): Buffer {
    val headerCopy = Header(this.header.fmt, this.header.csid, this.header.type, this.payload.length(),
        this.header.timestamp, this.header.streamId)
    return headerCopy.toBuffer().appendBuffer(this.payload)
  }

  companion object {

    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private fun toCommand(values: Array<Any?>, type: MessageType): Payload {
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
        CommandClose.NAME -> CommandClose(second, others)
        CommandFCUnpublilsh.NAME -> CommandFCUnpublilsh(second, others)
        CommandDeleteStream.NAME -> CommandDeleteStream(second, others)
        else -> {
          logger.warn("unsupported command name: {}", first)
          SimpleAMFPayload(values, type)
        }
      }
    }

    private fun toProtocolBandWidth(payload: Buffer): ProtocolBandWidth {
      val limitType = when {
        payload.length() > 4 -> payload.getUnsignedByte(4)
        else -> 0
      }
      return ProtocolBandWidth(payload.getUnsignedInt(0), limitType)
    }

  }

}