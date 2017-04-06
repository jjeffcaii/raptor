package `as`.leap.raptor.core

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.commons.Consts
import `as`.leap.raptor.core.impl.endpoint.LazyEndpoint
import `as`.leap.raptor.core.impl.ext.Endpoint
import `as`.leap.raptor.core.impl.ext.Handshaker
import `as`.leap.raptor.core.impl.ext.MessageFliper
import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.payload.CommandConnect
import `as`.leap.raptor.core.model.payload.CommandDeleteStream
import `as`.leap.raptor.core.model.payload.CommandFCUnpublilsh
import `as`.leap.raptor.core.model.payload.ProtocolChunkSize
import `as`.leap.raptor.core.utils.Do
import com.google.common.base.Preconditions
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetClient
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class Adaptor(
    private val netClient: NetClient,
    protected val address: Address,
    protected val chunkSize: Long,
    reconnect: Int = 0
) : Closeable {

  private var backend: Endpoint? = null

  protected val connected = AtomicBoolean(false)
  protected var transId: Int = 0
  private val retry = AtomicInteger(reconnect)

  protected var onConnect: Do? = null
  protected var onClose: Do? = null

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
        if (this.retry.decrementAndGet() < 0) {
          this.onClose?.invoke()
        } else {
          //TODO
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

  fun write(buffer: Buffer, strict: Boolean = true): Adaptor {
    if (strict) {
      Preconditions.checkArgument(this.connected(), "cannot write buffer because adaptor is disconnected.")
    }
    this.backend?.write(buffer)
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

  fun write(msg: Message): Adaptor {
    this.backend?.write(msg.toBuffer(this.chunkSize))
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
  }

  override fun close() {
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