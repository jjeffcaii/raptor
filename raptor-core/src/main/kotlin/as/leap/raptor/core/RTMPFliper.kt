package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.ChunkType
import `as`.leap.raptor.core.model.FMT
import `as`.leap.raptor.core.model.Handshake
import `as`.leap.raptor.core.model.Header
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.parsetools.RecordParser
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class RTMPFliper(
    private val parser: RecordParser,
    private val onHandshake: OnHandshake,
    private val onChunk: OnChunk,
    private var chunkSize: Int = 128
) : Handler<Buffer> {

  companion object {
    private val EMPTY_BUFFER = Buffer.buffer(0)
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

  private var state: ReadState = ReadState.HANDSHAKE0
  private var already: Int = 0
  // default chunk size
  private var fmt: FMT = FMT.F0
  private var csid: Int = 2
  private var timestamp: Int = 0
  private var extendTimestamp: Long? = null
  private var length: Int = 0
  private var type: ChunkType? = null
  private var streamid: Long = 0

  private var buffer1 = Buffer.buffer()
  private var buffer2 = Buffer.buffer()

  override fun handle(buffer: Buffer) {
    when (state) {
      ReadState.HANDSHAKE0 -> {
        this.emit(Handshake(buffer))
        this.state = ReadState.HANDSHAKE1
        this.parser.fixedSizeMode(1536)
      }
      ReadState.HANDSHAKE1 -> {
        this.emit(Handshake(buffer))
        this.state = ReadState.HANDSHAKE2
        this.parser.fixedSizeMode(1536)
      }
      ReadState.HANDSHAKE2 -> {
        this.emit(Handshake(buffer))
        this.state = ReadState.CHUNK_HEADER_BSC
        this.parser.fixedSizeMode(1)
      }
      ReadState.CHUNK_HEADER_BSC -> {
        this.buffer1 = buffer
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
        this.buffer1.appendBuffer(buffer)
        this.csid = buffer.getUnsignedByte(0) + 64
        this.fireMessageHeader()
      }
      ReadState.CHUNK_HEADER_BSC_1 -> {
        this.buffer1.appendBuffer(buffer)
        this.csid = buffer.getUnsignedShortLE(0) + 64
        this.fireMessageHeader()
      }
      ReadState.CHUNK_HEADER_MSG_11 -> {
        this.buffer2 = buffer
        this.already = 0
        this.timestamp = buffer.getUnsignedMedium(0)
        this.length = buffer.getUnsignedMedium(3)
        this.type = ChunkType.toChunkType(buffer.getUnsignedByte(6))
        this.streamid = buffer.getUnsignedIntLE(7)
        if (this.timestamp == 0x7FFFFF) {
          this.state = ReadState.CHUNK_EXT_TS
          this.parser.fixedSizeMode(4)
        } else {
          this.state = ReadState.CHUNK_BODY
          this.parser.fixedSizeMode(this.calcLength())
        }
      }
      ReadState.CHUNK_HEADER_MSG_7 -> {
        this.buffer2 = buffer
        this.already = 0
        this.timestamp = buffer.getUnsignedMedium(0)
        this.length = buffer.getUnsignedMedium(3)
        this.type = ChunkType.toChunkType(buffer.getUnsignedByte(6))
        if (this.timestamp == 0x7FFFFF) {
          this.state = ReadState.CHUNK_EXT_TS
          this.parser.fixedSizeMode(4)
        } else {
          this.state = ReadState.CHUNK_BODY
          this.parser.fixedSizeMode(this.calcLength())
        }
      }
      ReadState.CHUNK_HEADER_MSG_3 -> {
        this.buffer2 = buffer
        this.timestamp = buffer.getUnsignedMedium(0)
        if (this.timestamp == 0x7FFFFF) {
          this.state = ReadState.CHUNK_EXT_TS
          this.parser.fixedSizeMode(4)
        } else {
          this.state = ReadState.CHUNK_BODY
          this.parser.fixedSizeMode(this.calcLength())
        }
      }
      ReadState.CHUNK_HEADER_MSG_0 -> {
        this.buffer2 = null
        if (this.timestamp == 0x7FFFFF) {
          this.state = ReadState.CHUNK_EXT_TS
          this.parser.fixedSizeMode(4)
        } else {
          this.state = ReadState.CHUNK_BODY
          this.parser.fixedSizeMode(this.calcLength())
        }
      }
      ReadState.CHUNK_EXT_TS -> {
        //TODO process EXT_TS
        //this.cache.appendBuffer(buffer)
        this.extendTimestamp = buffer.getUnsignedInt(0)
        this.state = ReadState.CHUNK_BODY
        this.parser.fixedSizeMode(this.calcLength())
      }
      ReadState.CHUNK_BODY -> {
        // 1. reset chunk size if rcv set_chunk_size.
        if (this.fmt == FMT.F0 && this.csid == 2 && this.type == ChunkType.CTRL_SET_CHUNK_SIZE) {
          this.chunkSize = buffer.getUnsignedInt(0).toInt()
          if (logger.isDebugEnabled) {
            logger.debug("reset chunk size: {}", this.chunkSize)
          }
        }
        // 2. check extended timestamp.
        val timestamp: Long = if (this.timestamp == 0x7FFFFF) {
          this.extendTimestamp!!
        } else {
          this.timestamp.toLong()
        }
        // 3. emit chunk
        val header = Header(this.fmt, this.csid, timestamp, this.streamid, this.type!!, this.length)
        this.emit(Chunk(header, this.buffer1, this.buffer2, buffer))

        // 4. cleanup and next turn.
        this.buffer1 = null
        this.buffer2 = null
        this.state = ReadState.CHUNK_HEADER_BSC
        this.parser.fixedSizeMode(1)
      }
      else -> {
        throw UnsupportedOperationException("Not valid ReadState: ${this.state}.")
      }
    }
  }

  private fun fireMessageHeader() {
    when (this.fmt) {
      FMT.F0 -> {
        this.state = ReadState.CHUNK_HEADER_MSG_11
        this.parser.fixedSizeMode(11)
      }
      FMT.F1 -> {
        this.state = ReadState.CHUNK_HEADER_MSG_7
        this.parser.fixedSizeMode(7)
      }
      FMT.F2 -> {
        this.state = ReadState.CHUNK_HEADER_MSG_3
        this.parser.fixedSizeMode(3)
      }
      FMT.F3 -> {
        this.state = ReadState.CHUNK_HEADER_MSG_0
        this.handle(EMPTY_BUFFER)
      }
      else -> {
        throw UnsupportedOperationException("Not valid FMT: ${this.fmt}")
      }
    }
  }

  private fun calcLength(): Int {
    // 消息长度比默认块小, 返回消息长度
    if (this.length <= this.chunkSize) {
      this.already = this.length
      return this.length
    }
    // 剩余最后一个小块
    val left = this.length - this.already
    if (left < this.chunkSize) {
      this.already = this.length
      return left
    }
    // 按默认块大小
    this.already += this.chunkSize
    return this.chunkSize
  }

  private fun emit(handshake: Handshake) {
    this.onHandshake.invoke(handshake)
  }

  private fun emit(chunk: Chunk) {
    this.onChunk(chunk)
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
    CHUNK_HEADER_MSG_0,
    CHUNK_EXT_TS,
    CHUNK_BODY
  }
}