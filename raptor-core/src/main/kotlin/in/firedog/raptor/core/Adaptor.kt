package `in`.firedog.raptor.core

import `in`.firedog.raptor.api.Address
import `in`.firedog.raptor.commons.Consts
import `in`.firedog.raptor.core.impl.endpoint.LazyEndpoint
import `in`.firedog.raptor.core.impl.ext.Endpoint
import `in`.firedog.raptor.core.impl.ext.Handshaker
import `in`.firedog.raptor.core.impl.ext.MessageFliper
import `in`.firedog.raptor.core.model.*
import `in`.firedog.raptor.core.model.payload.CommandConnect
import `in`.firedog.raptor.core.model.payload.CommandDeleteStream
import `in`.firedog.raptor.core.model.payload.CommandFCUnpublilsh
import `in`.firedog.raptor.core.model.payload.ProtocolChunkSize
import `in`.firedog.raptor.core.utils.Do
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetClient
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicBoolean

/**
 * RTMP后端适配器
 */
abstract class Adaptor(
    private val netClient: NetClient,
    protected val address: Address,
    protected val chunkSize: Long,
    private val reconnect: Int = 0
) : Closeable {

  var backend: Endpoint? = null

  protected val connected = AtomicBoolean(false)
  protected var transId: Int = 0
  protected var onConnect: Do? = null
  protected var onClose: Do? = null

  private val isClosed = AtomicBoolean(false)
  private var retry = 0
  private val remembers: MutableList<Message> = mutableListOf()
  private var newborn = false
  private var bingoNext = false
  private var firstAudio: Chunk? = null
  private var firstVideo: Chunk? = null

  fun onConnect(cb: Do?): Adaptor {
    this.onConnect = cb
    return this
  }

  fun onClose(cb: Do?): Adaptor {
    this.onClose = cb
    return this
  }

  fun connect() {
    this.transId = 0
    val endpoint = LazyEndpoint(this.netClient, this.address.host, this.address.port)
    this.backend = endpoint
    val messages = MessageFliper()
    val hc = Handshaker(endpoint, {

      if (logger.isDebugEnabled) {
        logger.debug("handshake with {}:{} succes!", this.address.host, this.address.port)
      }

      if (this.chunkSize != Consts.RTMP_DEFAULT_CHUNK_SIZE) {
        this.sndChunkSize(this.chunkSize)
      }

      // send connect command.
      val cmdObj = mapOf(
          "app" to address.context,
          "type" to "nonprivate",
          "flashVer" to "FMLE/3.0 (compatible; FMSc/1.0)",
          "swfUrl" to address.toBaseURL(),
          "tcUrl" to address.toBaseURL()
      )
      val payload = CommandConnect(this.transId++, arrayOf(cmdObj))
      val header = Header(FMT.F0, 3, MessageType.COMMAND_AMF0)
      this.write(header, payload)
      // bind close event.
      endpoint.onClose {
        this.connected.set(false)
        if (!this.isClosed.get()) {
          if (++this.retry > this.reconnect) {
            logger.warn("endpoint is DEAD now!!!")
            this.isClosed.set(true)
            this.onClose?.invoke()
          } else {
            logger.warn("endpoint reconnect (retry {}/{}).", this.retry, this.reconnect)
            this.connect()
          }
        }
      }
    }, {
      logger.error("handshake failed: close backend.")
      endpoint.close()
    })

    endpoint.onChunk { messages.append(it) }.onHandshake { hc.validate(it) }

    messages.onMessage {
      when (it.header.type) {
        MessageType.COMMAND_AMF0, MessageType.COMMAND_AMF3 -> {
          this.onCommand(it)
        }
        else -> {
          //TODO 处理其他形式的消息体
        }
      }
    }
  }

  abstract fun onCommand(msg: Message)

  fun write(chunk: Chunk, strict: Boolean = true): Adaptor {
    if (!strict || this.connected()) {
      if (this.newborn) {
        // 处理重连后新生的适配器
        synchronized(this) {
          when (chunk.header.fmt) {
            FMT.F0 -> {
              this.backend?.write(chunk.toBuffer())
              this.newborn = false
              logger.info(">>> send chunk(0) success. your adaptor is reborn now!!!")
            }
            FMT.F1 -> {
              if (this.bingoNext) {
                val hd = Header.clone(chunk.header)
                hd.fmt = FMT.F0
                hd.streamId = 1
                this.write(hd.toBuffer().appendBuffer(chunk.payload))
                this.newborn = false
                logger.info(">>> hack chunk(1) success. your adaptor is reborn now!!!")
              } else if (chunk.payload.length() < this.chunkSize) {
                this.bingoNext = true
                logger.info(">>> skip broken media but next one will be bingo :-D")
              } else {
                logger.info(">>> skip broken media.")
              }
            }
            else -> {
              logger.warn(">>> newborn adaptor is waiting for chunk(0,1) but now is {}.", chunk.header.fmt.code)
            }
          }
        }
      } else {
        this.backend?.write(chunk.toBuffer())
        if (this.firstAudio == null && chunk.header.fmt == FMT.F0 && chunk.header.type == MessageType.MEDIA_AUDIO) {
          this.firstAudio = chunk
        }
        if (this.firstVideo == null && chunk.header.fmt == FMT.F0 && chunk.header.type == MessageType.MEDIA_VIDEO) {
          this.firstVideo = chunk
        }
      }
    }
    return this
  }

  fun write(buffer: Buffer, strict: Boolean = true): Adaptor {
    if (!strict || this.connected()) {
      this.backend?.write(buffer)
    }
    return this
  }

  fun write(header: Header, payload: Payload): Adaptor {
    val foo = payload.toBuffer()
    val bar = header.length
    header.length = foo.length()
    this.write(SimpleMessage(header, foo))
    header.length = bar
    return this
  }

  fun write(msg: Message, remember: Boolean = false): Adaptor {
    this.backend?.write(msg.toBuffer(this.chunkSize))
    if (remember) {
      this.remembers.add(msg)
    }
    return this
  }

  fun connected(): Boolean {
    return this.connected.get()
  }

  fun fireFCUnpublish(): Adaptor {
    val payload = CommandFCUnpublilsh(this.transId++, arrayOf(null, this.address.key))
    val header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
    this.write(header, payload)
    return this
  }

  fun fireDeleteStream(): Adaptor {
    val payload = CommandDeleteStream(this.transId++, arrayOf(null, 1))
    val header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
    this.write(header, payload)
    return this
  }

  protected fun ok() {
    this.connected.set(true)
    this.onConnect?.invoke()
    this.onConnect = null
    if (this.retry > 0) {
      synchronized(this) {
        this.remembers.forEach { this.write(it) }
        this.firstAudio?.let {
          this.write(it.toBuffer())
        }
        this.firstVideo?.let {
          this.write(it.toBuffer())
        }
        this.newborn = true
        this.bingoNext = false
      }
    }

    // mock adaptor close.
    /*
    if (retry < 1) {
      ForkJoinPool.commonPool().submit {
        Thread.sleep(RandomUtils.nextLong(3000L, 10000L))
        this.backend?.close()
        Thread.sleep(RandomUtils.nextLong(3000L, 10000L))
        this.backend?.close()
        Thread.sleep(RandomUtils.nextLong(3000L, 10000L))
        this.backend?.close()
        Thread.sleep(RandomUtils.nextLong(3000L, 10000L))
        this.backend?.close()
      }
    }
    */
  }

  override fun close() {
    this.isClosed.set(true)
    this.backend?.close()
  }

  private fun sndChunkSize(newSize: Long) {
    val header = Header.getProtocolHeader(MessageType.CTRL_SET_CHUNK_SIZE)
    val payload = ProtocolChunkSize(newSize)
    this.write(header.toBuffer().appendBuffer(payload.toBuffer()), false)
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

}