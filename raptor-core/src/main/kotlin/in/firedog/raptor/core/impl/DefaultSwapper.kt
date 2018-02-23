package `in`.firedog.raptor.core.impl

import `in`.firedog.raptor.api.Address
import `in`.firedog.raptor.api.NamespaceManager
import `in`.firedog.raptor.api.SecurityManager
import `in`.firedog.raptor.core.Swapper
import `in`.firedog.raptor.core.model.FMT
import `in`.firedog.raptor.core.model.Header
import `in`.firedog.raptor.core.model.MessageType
import `in`.firedog.raptor.core.model.Payload
import `in`.firedog.raptor.core.model.payload.*
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class DefaultSwapper(
    socket: NetSocket,
    netClient: NetClient,
    strategy: LiveStrategy = Swapper.LiveStrategy.ALL,
    reconnect: Int = 0,
    private val namespaceManager: NamespaceManager,
    private val securityManager: SecurityManager
) : Swapper(socket, netClient, strategy, reconnect) {

  override fun connect() {
    // 1. send onFCPublish command.
    val header: Header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
    var payload: Payload = CommandOnFCPublish(0, arrayOf(null, mapOf(
        "code" to "NetStream.Publish.Start",
        "description" to this.streamKey,
        "details" to this.streamKey,
        "clientid" to "0"
    )))
    this.write(header, payload)
    if (logger.isDebugEnabled) {
      logger.debug("<<<< send onFCPublish success!")
    }
    // 2. send onStatus command.
    payload = CommandOnStatus(5, arrayOf(null, ON_STATUS_CMD))
    header.fmt = FMT.F0
    header.csid = 4
    this.write(header, payload)
    if (logger.isDebugEnabled) {
      logger.debug("<<<< send onStatus success!")
    }
  }


  override fun handleCMD(cmd: CommandConnect) {
    val connInfo = cmd.getConnectInfo()
    val qux = Address.extractFull(connInfo.app)
    if (qux == null) {
      this.handleConnect(connInfo.app)
    } else {
      this.handleConnectInline(qux.first, qux.second)
    }
  }

  private fun handleConnect(ns: String) {
    if (!this.securityManager.exists(ns)) {
      logger.error("invalid application: {}.", ns)
      this.close()
      return
    }
    this.namespace = ns
    this.sendConnectSuccess()
  }

  private fun sendConnectSuccess() {
    // 1. send ack window size
    val b = Buffer.buffer()
    val header: Header = Header.getProtocolHeader(MessageType.CTRL_ACK_WINDOW_SIZE)
    var payload: Payload = ProtocolAckWindowSize()
    b.appendBuffer(header.toBuffer()).appendBuffer(payload.toBuffer())
    // 2. send set peer band width.
    header.type = MessageType.CTRL_SET_PEER_BANDWIDTH
    header.length = 5
    payload = ProtocolBandWidth(limit = 2)
    b.appendBuffer(header.toBuffer()).appendBuffer(payload.toBuffer())
    // 3. send set chunk size 1024
    header.type = MessageType.CTRL_SET_CHUNK_SIZE
    header.length = 4
    payload = ProtocolChunkSize(SND_CHUNK_SIZE)
    b.appendBuffer(header.toBuffer()).appendBuffer(payload.toBuffer())
    // 4. send _result
    payload = CommandResult(this.transId, arrayOf(CONNECTION_RESPONSE_CMD, CONNECTION_RESPONSE_INFO))
    val cmdBuffer = payload.toBuffer()
    header.fmt = FMT.F0
    header.csid = 3
    header.type = MessageType.COMMAND_AMF0
    header.length = cmdBuffer.length()
    b.appendBuffer(header.toBuffer()).appendBuffer(cmdBuffer)
    this.write(b)
  }

  private fun handleConnectInline(ns: String, streamKey: String) {
    if (!this.securityManager.exists(ns)) {
      logger.error("invalid application: {}.", ns)
      this.close()
      return
    }
    this.namespace = ns
    this.streamKey = streamKey
    val result = this.securityManager.validate(this.namespace, streamKey)
    if (!result.success) {
      logger.error("illegal stream key: namespace={}, streamKey={}.", this.namespace, this.streamKey)
      this.close()
      return
    }
    this.group = result.group
    this.sendConnectSuccess()
  }

  override fun handleCMD(cmd: CommandReleaseStream) {
    if (this.streamKey.isNullOrBlank()) {
      this.streamKey = cmd.getStreamKey()
      val result = this.securityManager.validate(this.namespace, streamKey)
      if (!result.success) {
        logger.error("illegal stream key: namespace={}, streamKey={}.", this.namespace, this.streamKey)
        this.close()
        return
      }
      this.group = result.group
    }

    val addresses = this.namespaceManager.load(this.namespace, this.group)
    // no address binding.
    if (addresses.isEmpty()) {
      logger.error("missing addresses: namespace={}, group={}.", this.namespace, this.group)
      this.close()
      return
    }
    addresses.forEach { this.establish(it) }
    val header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
    val payload = CommandResult(this.transId, arrayOf(null, 1))
    this.write(header, payload)
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private val SND_CHUNK_SIZE = 1024L
    private val CONNECTION_RESPONSE_CMD = mapOf(
        "fmsVer" to "FMS/3,0,1,123",
        "capabilities" to 31
    )
    private val CONNECTION_RESPONSE_INFO = mapOf(
        "level" to "status",
        "code" to "NetConnection.Connect.Success",
        "description" to "Connection successed.",
        "objectEncoding" to 0
    )
    private val ON_STATUS_CMD = mapOf(
        "level" to "status",
        "code" to "NetStream.Publish.Start",
        "description" to "Start Publishing",
        "objectEncoding" to 0
    )
  }

}