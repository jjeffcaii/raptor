package `as`.leap.raptor.core

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.commons.Consts
import `as`.leap.raptor.core.impl.DefaultAdaptor
import `as`.leap.raptor.core.impl.endpoint.DirectEndpoint
import `as`.leap.raptor.core.impl.ext.Endpoint
import `as`.leap.raptor.core.impl.ext.Handshaker
import `as`.leap.raptor.core.impl.ext.MessageFliper
import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.payload.*
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetSocket
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicInteger

abstract class Swapper(
    socket: NetSocket,
    private val netClient: NetClient,
    private val strategy: LiveStrategy = Swapper.LiveStrategy.ALL
) : Closeable {

  private val endpoint: Endpoint
  protected var chunkSize: Long = Consts.RTMP_DEFAULT_CHUNK_SIZE
  protected var namespace: String = StringUtils.EMPTY
  protected var group: String = StringUtils.EMPTY

  protected var streamKey: String = StringUtils.EMPTY

  protected var transId: Int = 0

  private val adaptors: MutableList<Adaptor> = mutableListOf()
  private val connects = AtomicInteger(0)

  abstract protected fun handleCMD(cmd: CommandReleaseStream)
  abstract protected fun handleCMD(cmd: CommandConnect)
  abstract protected fun connect()

  protected fun write(buffer: Buffer): Swapper {
    this.endpoint.write(buffer)
    return this
  }

  protected fun write(header: Header, payload: Payload): Swapper {
    val b = payload.toBuffer()
    val tmp = header.length
    header.length = b.length()
    this.write(header.toBuffer().appendBuffer(b))
    header.length = tmp
    return this
  }

  protected fun establish(address: Address) {
    val adaptor = DefaultAdaptor(this.netClient, address, this.chunkSize)
    // bind connect
    adaptor.onConnect {
      logger.info("establish success: {}", address)
      if (this.connects.incrementAndGet() == this.adaptors.size) {
        this.connect()
        logger.info("**** start publishing! ****")
      }
    }
    // bind close
    adaptor.onClose {
      logger.warn("connection dead: {}", address)
      val lives = this.connects.decrementAndGet()
      when (this.strategy) {
        LiveStrategy.ALL -> {
          logger.warn("swapper closed: strategy=ALL, need={}, alive={}.", this.adaptors.size, lives)
          this.close()
        }
        LiveStrategy.QUORUM -> {
          val need = this.adaptors.size / 2 + 1
          if (lives < need) {
            logger.warn("swapper closed: strategy=QUORUM, need={}, alive={}.", need, lives)
            this.close()
          }
        }
        LiveStrategy.ANY -> {
          if (lives < 1) {
            logger.warn("swapper closed: strategy=ANY, need=1, alive=0.")
            this.close()
          }
        }
      }
    }
    // fire connect
    adaptor.connect()

    if (logger.isDebugEnabled) {
      logger.debug("establish to {}......", address)
    }

    this.adaptors.add(adaptor)
  }

  override fun close() {
    this.endpoint.close()
    this.adaptors.forEach(Adaptor::close)
  }

  enum class LiveStrategy {
    ANY, QUORUM, ALL
  }

  init {
    this.endpoint = DirectEndpoint(socket)
    val messages = MessageFliper()
    messages.onMessage {
      when (it.header.type) {
        MessageType.CTRL_SET_CHUNK_SIZE -> this.chunkSize = (it.toModel() as ProtocolChunkSize).chunkSize
        MessageType.DATA_AMF0, MessageType.DATA_AMF3 -> this.sndSetDataFrame(it)
        MessageType.COMMAND_AMF3, MessageType.COMMAND_AMF0 -> {
          this.transId++
          val model = it.toModel()
          when (model) {
            is CommandConnect -> this.handleCMD(model)
            is CommandReleaseStream -> this.handleCMD(model)
            is CommandFCPublish -> this.handleCMD(model)
            is CommandCreateStream -> this.handleCMD(model)
            is CommandCheckBW -> this.handleCMD(model)
            is CommandPublish -> logger.info("**** waiting for {} adaptors connect... ****", this.adaptors.size)
            is CommandFCUnpublilsh -> this.handleCMD(model)
            is CommandDeleteStream -> this.handleCMD(model)
            else -> logger.info("other commans: {}", model)
          }
        }
        else -> {
          // ignore
          if (logger.isDebugEnabled) {
            logger.debug("other message type: {}.", it.header.type)
          }
        }
      }
    }
    val handshaker = Handshaker(this.endpoint, failed = { this.close() }, passive = true)
    this.endpoint
        .onHandshake { handshaker.validate(it) }
        .onChunk { chunk ->
          if (chunk.header.type.isMedia()) {
            this.adaptors.forEach { it.write(chunk.toBuffer()) }
          } else {
            messages.append(chunk)
          }
        }
        .onClose {
          this.close()
        }
  }

  private fun handleCMD(cmd: CommandFCUnpublilsh) {
    if (logger.isDebugEnabled) {
      logger.debug("handle cmd {}: {}", CommandFCUnpublilsh.NAME, cmd)
    }
    this.adaptors.forEach { it.fireFCUnpublish() }
  }

  private fun handleCMD(cmd: CommandDeleteStream) {
    if (logger.isDebugEnabled) {
      logger.debug("handle cmd {}: {}", CommandDeleteStream.NAME, cmd)
    }
    this.adaptors.forEach { it.fireDeleteStream() }
  }

  private fun handleCMD(cmd: CommandFCPublish) {
    if (logger.isDebugEnabled) {
      logger.debug("handle cmd {}: {}", CommandFCPublish.NAME, cmd)
    }
    val streamKey = this.streamKey
    val infoObj = mapOf(
        "code" to "NetStream.Publish.Start",
        "description" to streamKey,
        "details" to streamKey,
        "clientid" to "0"
    )
    val header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
    val payload = CommandOnFCPublish(this.transId, arrayOf(null, infoObj))
    this.write(header, payload)
  }

  private fun handleCMD(cmd: CommandCreateStream) {
    if (logger.isDebugEnabled) {
      logger.debug("handle cmd {}: {}", CommandCreateStream.NAME, cmd)
    }
    val header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
    val payload = CommandResult(this.transId, arrayOf(null, 1))
    this.write(header, payload)
  }

  private fun handleCMD(cmd: CommandCheckBW) {
    if (logger.isDebugEnabled) {
      logger.debug("handle cmd {}: {}", CommandCheckBW.NAME, cmd)
    }
    val header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
    val payload = CommandResult(this.transId, arrayOf(null, 1))
    this.write(header, payload)
  }

  private fun sndSetDataFrame(msg: Message) {
    val payload = msg.toModel() as SimpleAMFPayload
    val first = payload.body.first()
    if (first is String && SET_DATA_FRAME == first) {
      this.adaptors.forEach { it.write(msg) }
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private val SET_DATA_FRAME = "@setDataFrame"
  }

}