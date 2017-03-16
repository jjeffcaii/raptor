package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.ChunkType
import `as`.leap.raptor.core.model.FMT
import `as`.leap.raptor.core.model.Header
import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.msg.Chunk
import `as`.leap.raptor.core.model.msg.Handshake0
import `as`.leap.raptor.core.model.msg.Handshake1
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.parsetools.RecordParser
import net.engio.mbassy.bus.MBassador
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles


class MessageFliper(private val parser: RecordParser, sub: (Message<Any>) -> Unit) : Handler<Buffer> {

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

  private val bus: MBassador<Message<Any>>
  private var state: ReadState = ReadState.HANDSHAKE0
  private var already: Int = 0
  // default chunk size
  private var chunkSize: Int = 4096
  private var fmt: FMT = FMT._0
  private var csid: Int = 2
  private var ts: Int = 0
  private var ets: Long? = null
  private var len: Int? = null
  private var type: ChunkType? = null
  private var streamid: Long = 0
  private var cache: Buffer = Buffer.buffer()

  init {
    this.bus = MBassador<Message<Any>>({ error ->
      logger.error("event bus error.", error)
    })
    this.bus.subscribe(MessageListener(sub))
  }

  private fun emit(msg: Message<Any>) {
    if (logger.isDebugEnabled) {
      logger.debug("emit message: {} bytes.", msg.toBuffer().length())
    }
    this.bus.publishAsync(msg)
  }

  override fun handle(buffer: Buffer) {
    if (logger.isDebugEnabled) {
      logger.debug("handle {} bytes.", buffer.length())
    }
    when (state) {
      ReadState.HANDSHAKE0 -> {
        this.emit(Handshake0(buffer))
        this.state = ReadState.HANDSHAKE1
        this.parser.fixedSizeMode(1536)
      }
      ReadState.HANDSHAKE1 -> {
        this.emit(Handshake1(buffer))
        this.state = ReadState.HANDSHAKE2
        this.parser.fixedSizeMode(1536)
      }
      ReadState.HANDSHAKE2 -> {
        this.emit(Handshake1(buffer))
        this.state = ReadState.CHUNK_HEADER_BSC
        this.parser.fixedSizeMode(1)
      }
      ReadState.CHUNK_HEADER_BSC -> {
        this.cache.appendBuffer(buffer)
        val b = buffer.getByte(0)
        this.fmt = FMT.valueOf(b)
        this.csid = b.toInt() and 0x3F
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
            this.fireMessageHeader()
          }
        }
      }
      ReadState.CHUNK_HEADER_BSC_0 -> {
        this.cache.appendBuffer(buffer)
        this.csid = buffer.getByte(0) + 64
        this.fireMessageHeader()
      }
      ReadState.CHUNK_HEADER_BSC_1 -> {
        this.cache.appendBuffer(buffer)
        this.csid = buffer.getUnsignedShortLE(0) + 64
        this.fireMessageHeader()
      }
      ReadState.CHUNK_HEADER_MSG_11 -> {
        this.cache.appendBuffer(buffer)
        this.already = 0
        this.ts = buffer.getUnsignedMedium(0)
        this.len = buffer.getUnsignedMedium(3)
        this.type = ChunkType.toChunkType(buffer.getUnsignedByte(6))
        this.streamid = buffer.getUnsignedIntLE(7)
        if (this.ts == 0x7FFFFF) {
          this.state = ReadState.CHUNK_EXT_TS
          this.parser.fixedSizeMode(4)
        } else {
          this.state = ReadState.CHUNK_BODY
          this.parser.fixedSizeMode(this.calculatePayloadLength())
        }
      }
      ReadState.CHUNK_HEADER_MSG_7 -> {
        this.cache.appendBuffer(buffer)
        this.already = 0
        this.ts = buffer.getUnsignedMedium(0)
        this.len = buffer.getUnsignedMedium(3)
        this.type = ChunkType.toChunkType(buffer.getUnsignedByte(6))
        if (this.ts == 0x7FFFFF) {
          this.state = ReadState.CHUNK_EXT_TS
          this.parser.fixedSizeMode(4)
        } else {
          this.state = ReadState.CHUNK_BODY
          this.parser.fixedSizeMode(this.calculatePayloadLength())
        }
      }
      ReadState.CHUNK_HEADER_MSG_3 -> {
        this.cache.appendBuffer(buffer)
        this.ts = buffer.getUnsignedMedium(0)
        this.state = ReadState.CHUNK_BODY
        this.parser.fixedSizeMode(Math.min(this.chunkSize, this.len!!))
        if (this.ts == 0x7FFFFF) {
          this.state = ReadState.CHUNK_EXT_TS
          this.parser.fixedSizeMode(4)
        } else {
          this.state = ReadState.CHUNK_BODY
          this.parser.fixedSizeMode(this.calculatePayloadLength())
        }
      }
      ReadState.CHUNK_EXT_TS -> {
        this.cache.appendBuffer(buffer)
        this.ets = buffer.getUnsignedInt(0)
        this.state = ReadState.CHUNK_BODY
        this.parser.fixedSizeMode(this.calculatePayloadLength())
      }
      ReadState.CHUNK_BODY -> {
        this.cache.appendBuffer(buffer)
        val timestamp: Long
        if (this.ts == 0x7FFFFF) {
          timestamp = this.ets!!
        } else {
          timestamp = this.ts.toLong()
        }
        val header = Header(this.fmt, this.csid, timestamp, this.streamid, this.type!!, this.len)
        logger.info("flip header: {}", header)
        this.emit(Chunk(this.cache, header))
        this.cache = Buffer.buffer()
        this.state = ReadState.CHUNK_HEADER_BSC
        this.parser.fixedSizeMode(1)
      }
      else -> {
        throw UnsupportedOperationException("Not valid ReadState: ${this.state}")
      }
    }
  }

  private fun fireMessageHeader() {
    when (this.fmt) {
      FMT._0 -> {
        this.state = ReadState.CHUNK_HEADER_MSG_11
        this.parser.fixedSizeMode(11)
      }
      FMT._1 -> {
        this.state = ReadState.CHUNK_HEADER_MSG_7
        this.parser.fixedSizeMode(7)
      }
      FMT._2 -> {
        this.state = ReadState.CHUNK_HEADER_MSG_3
        this.parser.fixedSizeMode(3)
      }
      FMT._3 -> {
        this.state = ReadState.CHUNK_BODY
        this.parser.fixedSizeMode(this.calculatePayloadLength())
      }
      else -> {
        throw UnsupportedOperationException("Not valid FMT: ${this.fmt}")
      }
    }
  }

  private fun calculatePayloadLength(): Int {
    val ret: Int
    val l = this.len!!
    if (l <= this.chunkSize) {
      ret = l
    } else {
      val d = l - this.already
      ret = Math.min(d, this.chunkSize)
      if (d < this.chunkSize) {
        this.already += this.chunkSize
      }
    }
    logger.info("calculate length: {}", ret)
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