package `as`.leap.raptor.core.impl

import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.core.Swapper
import `as`.leap.raptor.core.model.FMT
import `as`.leap.raptor.core.model.Header
import `as`.leap.raptor.core.model.MessageType
import `as`.leap.raptor.core.model.Payload
import `as`.leap.raptor.core.model.payload.*
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class OBSSwapper(socket: NetSocket, namespaces: NamespaceManager) : Swapper(socket, namespaces) {

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
    this.namespace = cmd.getConnectInfo().app
    if (!this.namespaces.exists(this.namespace)) {
      //TODO invalid app
      logger.error("invalid application: {}.", this.namespace)
      this.close()
      return
    }
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

  override fun handleCMD(cmd: CommandReleaseStream) {
    this.streamKey = cmd.getStreamKey()
    val addresses = this.namespaces.address(this.namespace, this.streamKey)

    // no address binding.
    if (addresses.isEmpty()) {
      logger.error("cannot find any RTMP stream address: streamKey={}.", this.streamKey)
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