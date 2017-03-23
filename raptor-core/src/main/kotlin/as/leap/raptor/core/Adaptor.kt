package `as`.leap.raptor.core

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.core.endpoint.Backend
import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.payload.CommandConnect
import `as`.leap.raptor.core.model.payload.ProtocolChunkSize
import `as`.leap.raptor.core.utils.Do
import com.google.common.base.Preconditions
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicBoolean

abstract class Adaptor : Closeable {

  protected val address: Address
  protected val chunkSize: Long
  protected val backend: Endpoint
  protected val onConnect: Do?
  protected val connected = AtomicBoolean(false)

  constructor(address: Address, chunkSize: Long, onConnect: Do?) {
    this.address = address
    this.chunkSize = chunkSize
    this.onConnect = onConnect
    this.backend = Backend(this.address.host, this.address.port)
    val messages = MessageFliper()
    val hc = HandshakeContext(backend, {
      if (logger.isDebugEnabled) {
        logger.debug("handshake with {}:{} succes!", this.address.host, this.address.port)
      }
      //TODO 处理握手成功后续响应
      // send set chunk size.
      var header: Header = Header.getProtocolHeader(MessageType.CTRL_SET_CHUNK_SIZE)
      var payload: Payload = ProtocolChunkSize(this.chunkSize)
      var b: Buffer = payload.toBuffer()
      backend.write(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(b))

      // send connect command.
      val cmdObj = mapOf(
          "app" to address.context,
          "type" to "nonprivate",
          "flashVer" to "FMLE/3.0 (compatible; FMSc/1.0)",
          "swfUrl" to address.toBaseURL(),
          "tcUrl" to address.toBaseURL()
      )
      payload = CommandConnect(1, arrayOf(cmdObj))
      b = payload.toBuffer()
      header = Header(FMT.F0, 3, 0L, 0L, MessageType.COMMAND_AMF0, b.length())
      backend.write(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(b))
    }, {
      this.close()
    })

    backend
        .onChunk {
          messages.append(it)
        }
        .onHandshake {
          hc.check(it)
        }

    messages.onMessage {
      //TODO 处理来自backend的消息
      logger.info("<<< rcv: {}", it.header.type)
      when (it.header.type) {
        MessageType.COMMAND_AMF0, MessageType.COMMAND_AMF3 -> {
          this.onCommand(it)
        }
        else -> {
        }
      }
    }

  }

  abstract fun onCommand(msg: Message)

  fun write(buffer: Buffer): Adaptor {
    Preconditions.checkArgument(this.connected(), "cannot write buffer because adaptor is disconnected.")
    this.backend.write(buffer)
    return this
  }

  fun connected(): Boolean {
    return this.connected.get()
  }

  override fun close() {
    this.backend.close()
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

}