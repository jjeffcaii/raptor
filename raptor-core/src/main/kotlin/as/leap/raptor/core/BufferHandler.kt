package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.ChunkType
import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.MessageType
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.parsetools.RecordParser
import net.engio.mbassy.bus.MBassador
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import kotlin.experimental.and


class BufferHandler : Handler<Buffer> {

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

  private val parser: RecordParser
  private var bus = MBassador<Message>({ error ->
    logger.error("event bus error.", error)
  })
  private var state: ReadState = ReadState.HANDSHAKE0
  private var already: Int = 0
  // default chunk size
  private var chunkSize: Int = 4096
  private var fmt: Int? = null // 0,1,2,3
  private var csid: Int? = null
  private var ts: Int? = null
  private var ets: Long? = null
  private var len: Int? = null
  private var type: ChunkType? = null
  private var streamid: Long? = null
  private var headerBuffer: Buffer? = null

  constructor(parser: RecordParser, sub: (Message) -> Unit) {
    this.parser = parser
    this.bus.subscribe(MessageListener(sub))
  }

  private fun emit(msg: Message) {
    logger.debug("emit message: {} bytes.", msg.buffer().length())
    this.bus.post(msg)
  }

  override fun handle(buffer: Buffer) {
    if (logger.isDebugEnabled) {
      logger.debug("handle {} bytes.", buffer.length())
    }
    when (state) {
      ReadState.HANDSHAKE0 -> {
        this.emit(Message(MessageType.HANDSHAKE, buffer.slice(0, 1)))
        this.state = ReadState.HANDSHAKE1
        this.parser.fixedSizeMode(1536)
      }
      ReadState.HANDSHAKE1 -> {
        this.emit(Message(MessageType.HANDSHAKE, buffer.slice(0, 1536)))
        this.state = ReadState.HANDSHAKE2
        this.parser.fixedSizeMode(1536)
      }
      ReadState.HANDSHAKE2 -> {
        this.emit(Message(MessageType.HANDSHAKE, buffer.slice(0, 1536)))
        this.state = ReadState.CHUNK_HEADER_BSC
        this.parser.fixedSizeMode(1)
      }
      ReadState.CHUNK_HEADER_BSC -> {
        val buff = Buffer.buffer()
        buff.appendBuffer(buffer)
        this.headerBuffer = buff
        val b = buffer.getByte(0)
        this.fmt = b.toInt().shr(6)
        this.csid = (b and 0x3F).toInt()
        when (this.csid) {
          0 -> {
            this.state = ReadState.CHUNK_HEADER_BSC_0
            this.parser.fixedSizeMode(1)
          }
          1 -> {
            this.state = ReadState.CHUNK_HEADER_BSC_1
            this.parser.fixedSizeMode(2)
          }
          else -> {
            when (this.fmt) {
              0 -> {
                this.state = ReadState.CHUNK_HEADER_MSG_11
                this.parser.fixedSizeMode(11)
              }
              1 -> {
                this.state = ReadState.CHUNK_HEADER_MSG_7
                this.parser.fixedSizeMode(7)
              }
              2 -> {
                this.state = ReadState.CHUNK_HEADER_MSG_3
                this.parser.fixedSizeMode(3)
              }
              3 -> {
                this.state = ReadState.CHUNK_BODY
                this.parser.fixedSizeMode(this.calculatePayloadLength())
              }
            }
          }
        }
      }
      ReadState.CHUNK_HEADER_MSG_11 -> {
        this.headerBuffer!!.appendBuffer(buffer)
        this.already = 0
        this.ts = buffer.getUnsignedShort(0) * 256 + buffer.getByte(2)
        this.len = buffer.getUnsignedShort(3) * 256 + buffer.getByte(5)
        this.type = ChunkType.toChunkType(buffer.getByte(6))
        this.streamid = buffer.getUnsignedIntLE(7)
        if (this.ts!! == 0x7FFFFF) {
          this.state = ReadState.CHUNK_EXT_TS
          this.parser.fixedSizeMode(4)
        } else {
          this.state = ReadState.CHUNK_BODY
          this.parser.fixedSizeMode(this.calculatePayloadLength())
        }
      }
      ReadState.CHUNK_HEADER_MSG_7 -> {
        this.headerBuffer!!.appendBuffer(buffer)
        this.already = 0
        this.ts = buffer.getUnsignedShort(0) * 256 + buffer.getByte(2)
        this.len = buffer.getUnsignedShort(3) * 256 + buffer.getByte(5)
        this.type = ChunkType.toChunkType(buffer.getByte(6))
        if (this.ts!! == 0x7FFFFF) {
          this.state = ReadState.CHUNK_EXT_TS
          this.parser.fixedSizeMode(4)
        } else {
          this.state = ReadState.CHUNK_BODY
          this.parser.fixedSizeMode(this.calculatePayloadLength())
        }
      }
      ReadState.CHUNK_HEADER_MSG_3 -> {
        this.headerBuffer!!.appendBuffer(buffer)
        this.ts = buffer.getUnsignedShort(0) * 256 + buffer.getByte(2)
        this.state = ReadState.CHUNK_BODY
        this.parser.fixedSizeMode(Math.min(this.chunkSize, this.len!!))
        if (this.ts!! == 0x7FFFFF) {
          this.state = ReadState.CHUNK_EXT_TS
          this.parser.fixedSizeMode(4)
        } else {
          this.state = ReadState.CHUNK_BODY
          this.parser.fixedSizeMode(this.calculatePayloadLength())
        }
      }
      ReadState.CHUNK_EXT_TS -> {
        this.headerBuffer!!.appendBuffer(buffer)
        this.ets = buffer.getUnsignedInt(0)
        this.state = ReadState.CHUNK_BODY
        this.parser.fixedSizeMode(this.calculatePayloadLength())
      }
      ReadState.CHUNK_BODY -> {
        val buff = headerBuffer!!
        buff.appendBuffer(buffer)
        this.emit(Message(MessageType.CHUNK, buff))
        this.headerBuffer = null
        this.state = ReadState.CHUNK_HEADER_BSC
        this.parser.fixedSizeMode(1)
      }
      else -> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
      }
    }
  }

  private fun calculatePayloadLength(): Int {
    val ret: Int
    val l = this.len ?: 0
    if (l > this.chunkSize) {
      ret = l
    } else {
      val d = l - this.already
      ret = Math.min(d, this.chunkSize)
      if (d < this.chunkSize) {
        this.already += this.chunkSize
      }
    }
    return ret
  }

  private enum class ReadState {
    HANDSHAKE0,
    HANDSHAKE1,
    HANDSHAKE2,
    CHUNK_HEADER_BSC,
    CHUNK_HEADER_BSC_0,
    CHUNK_HEADER_BSC_1,
    CHUNK_HEADER_MSG_11,
    CHUNK_HEADER_MSG_7,
    CHUNK_HEADER_MSG_3,
    CHUNK_EXT_TS,
    CHUNK_BODY
  }


}