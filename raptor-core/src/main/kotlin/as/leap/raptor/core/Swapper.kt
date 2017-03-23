package `as`.leap.raptor.core

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.core.adaptor.QiniuAdaptor
import `as`.leap.raptor.core.endpoint.Frontend
import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.MessageType
import `as`.leap.raptor.core.model.payload.ProtocolChunkSize
import `as`.leap.raptor.core.model.payload.SimpleAMFPayload
import io.vertx.core.net.NetSocket
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicInteger

abstract class Swapper(socket: NetSocket, protected val namespaces: NamespaceManager) : Closeable {

  protected val endpoint: Endpoint
  protected var chunkSize: Long = 128
  protected var namespace: String = StringUtils.EMPTY
  protected var streamKey: String = StringUtils.EMPTY
  protected val adaptors: MutableList<Adaptor> = mutableListOf()

  private val connects = AtomicInteger(0)

  abstract protected fun onCommand(msg: Message)

  abstract protected fun connect()

  protected fun establish(address: Address) {
    val adaptor = when (address.provider) {
      Address.Provider.QINIU -> {
        QiniuAdaptor(address, this.chunkSize, {
          logger.info("establish success: {}", address)
          if (this.connects.incrementAndGet() == this.adaptors.size) {
            this.connect()
            logger.info("**** start publishing! ****")
          }
        }, {
          logger.warn("some backend closed. close all endpoints.")
          this.close()
        })
      }
      else -> {
        TODO("other provider adaptor")
      }
    }
    this.adaptors.add(adaptor)
  }

  override fun close() {
    this.endpoint.close()
    this.adaptors.forEach(Adaptor::close)
  }

  init {
    socket.pause()
    this.endpoint = Frontend(socket)
    val messages = MessageFliper()
    messages.onMessage {
      when (it.header.type) {
        MessageType.CTRL_SET_CHUNK_SIZE -> this.chunkSize = (it.toModel() as ProtocolChunkSize).chunkSize
        MessageType.COMMAND_AMF3, MessageType.COMMAND_AMF0 -> this.onCommand(it)
        MessageType.DATA_AMF0, MessageType.DATA_AMF3 -> this.trySetDataFrame(it)
        else -> {
          // ignore
        }
      }
    }
    val handshaker = HandshakeContext(this.endpoint, failed = { this.close() }, passive = true)
    this.endpoint
        .onHandshake { handshaker.check(it) }
        .onChunk {
          if (it.header.type.isMedia()) {
            val buffer = it.toBuffer()
            this.adaptors.forEach { it.write(buffer) }
          } else {
            messages.append(it)
          }
        }
        .onClose {
          this.close()
        }
    socket.resume()
  }

  private fun trySetDataFrame(msg: Message) {
    val payload = msg.toModel() as SimpleAMFPayload
    val first = payload.body[0]
    if (first is String && StringUtils.equals(SET_DATA_FRAME, first)) {
      this.adaptors.forEach {
        it.write(msg.toBuffer())
      }
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private val SET_DATA_FRAME = "@setDataFrame"
  }

}