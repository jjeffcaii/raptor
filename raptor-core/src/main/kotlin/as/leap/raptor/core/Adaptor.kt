package `as`.leap.raptor.core

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.core.endpoint.LazyEndpoint
import `as`.leap.raptor.core.ext.Endpoint
import `as`.leap.raptor.core.ext.Handshaker
import `as`.leap.raptor.core.ext.MessageFliper
import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.payload.CommandConnect
import `as`.leap.raptor.core.model.payload.ProtocolChunkSize
import `as`.leap.raptor.core.utils.Do
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicBoolean

abstract class Adaptor : Closeable {

  protected val address: Address
  protected val chunkSize: Long
  private val backend: Endpoint
  protected val onConnect: Do?
  protected val onClose: Do?
  protected val connected = AtomicBoolean(false)

  private fun sndChunkSize(newSize: Long) {
    val header = Header.getProtocolHeader(MessageType.CTRL_SET_CHUNK_SIZE)
    val payload = ProtocolChunkSize(newSize)
    this.write(header.toBuffer().appendBuffer(payload.toBuffer()))
  }

  constructor(address: Address, chunkSize: Long, onConnect: Do?, onClose: Do?) {
    this.address = address
    this.chunkSize = chunkSize
    this.onConnect = onConnect
    this.onClose = onClose
    this.backend = LazyEndpoint(this.address.host, this.address.port)
    val messages = MessageFliper()
    val hc = Handshaker(backend, {

      if (logger.isDebugEnabled) {
        logger.debug("handshake with {}:{} succes!", this.address.host, this.address.port)
      }

      if (this.chunkSize != 128L) {
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
      val payload = CommandConnect(1, arrayOf(cmdObj))
      val header = Header(FMT.F0, 3, MessageType.COMMAND_AMF0)
      this.write(header, payload)

      // bind close event.
      backend.onClose {
        this.onClose?.invoke()
      }
    }, {
      logger.error("handshake failed: close backend.")
      this.close()
      this.onClose?.invoke()
    })

    backend.onChunk { messages.append(it) }.onHandshake { hc.validate(it) }

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

  fun write(buffer: Buffer): Adaptor {
    //Preconditions.checkArgument(this.connected(), "cannot write buffer because adaptor is disconnected.")
    if (this.connected()) {
      this.backend.write(buffer)
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

  fun write(msg: Message): Adaptor {
    this.backend.write(msg.toBuffer(this.chunkSize))
    return this
  }

  fun connected(): Boolean {
    return this.connected.get()
  }

  protected fun ok() {
    this.connected.set(true)
    this.onConnect?.invoke()
  }

  override fun close() {
    this.backend.close()
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

}