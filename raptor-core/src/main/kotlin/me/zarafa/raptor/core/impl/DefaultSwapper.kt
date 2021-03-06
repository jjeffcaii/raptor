package me.zarafa.raptor.core.impl

import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetSocket
import me.zarafa.raptor.api.Address
import me.zarafa.raptor.api.ChannelManager
import me.zarafa.raptor.core.Swapper
import me.zarafa.raptor.core.model.FMT
import me.zarafa.raptor.core.model.Header
import me.zarafa.raptor.core.model.MessageType
import me.zarafa.raptor.core.model.Payload
import me.zarafa.raptor.core.model.payload.*
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class DefaultSwapper(
    socket: NetSocket,
    netClient: NetClient,
    strategy: LiveStrategy = Swapper.LiveStrategy.ALL,
    reconnect: Int = 0,
    private val channelManager: ChannelManager
) : Swapper(socket, netClient, strategy, reconnect) {

  override fun connect() {
    // 1. send onFCPublish command.
    val header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
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

  private fun handleConnect(channel: String) {
    if (!this.channelManager.exists(channel)) {
      logger.error("invalid application: {}.", channel)
      this.close()
      return
    }
    this.channel = channel
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
    if (!this.channelManager.exists(ns)) {
      logger.error("invalid channel: {}.", ns)
      this.close()
      return
    }
    this.channel = ns
    this.streamKey = streamKey

    if (!this.channelManager.validate(this.channel, streamKey)) {
      logger.error("illegal stream key: channel={}, streamKey={}.", this.channel, this.streamKey)
      this.close()
      return
    }
    this.sendConnectSuccess()
  }

  override fun handleCMD(cmd: CommandReleaseStream) {
    if (this.streamKey.isBlank()) {
      this.streamKey = cmd.getStreamKey()
      if (!this.channelManager.validate(this.channel, streamKey)) {
        logger.error("illegal stream key: channel={}, streamKey={}.", this.channel, this.streamKey)
        this.close()
        return
      }
    }

    val addresses = this.channelManager.load(this.channel)
    // no address binding.
    if (addresses.isEmpty()) {
      logger.error("missing addresses: channel={}.", this.channel)
      this.close()
      return
    }
    addresses.forEach { this.establish(it) }
    val header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
    val payload = CommandResult(this.transId, arrayOf(null, 1))
    this.write(header, payload)
  }

  companion object {
    private const val SND_CHUNK_SIZE = 1024L
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
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