package `as`.leap.raptor.core.rocket

import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.core.Endpoint
import `as`.leap.raptor.core.Swapper
import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.payload.*
import flex.messaging.io.amf.ASObject
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class OBSSwapper(endpoint: Endpoint, namespaces: NamespaceManager) : Swapper(endpoint, namespaces) {

  override fun connect() {
    // 1. send onFCPublish command.
    val header: Header = Header(FMT.F1, 3, 0L, 0L, MessageType.COMMAND_AMF0, 0)
    var payload: Payload = CommandOnFCPublish(0, arrayOf(null, mapOf(
        "code" to "NetStream.Publish.Start",
        "description" to this.streamKey,
        "details" to this.streamKey,
        "clientid" to "0"
    )))
    var buffer: Buffer = payload.toBuffer()
    header.length = buffer.length()
    this.endpoint.write(header.toBuffer().appendBuffer(buffer))

    if (logger.isDebugEnabled) {
      logger.debug("<<<< onFCPublish")
    }

    // 2. send onStatus command.
    val infoObj = mapOf(
        "level" to "status",
        "code" to "NetStream.Publish.Start",
        "description" to "Start Publishing",
        "objectEncoding" to 0
    )
    payload = CommandOnStatus(5, arrayOf(null, infoObj))
    buffer = payload.toBuffer()
    header.fmt = FMT.F0
    header.csid = 4
    header.length = buffer.length()
    this.endpoint.write(header.toBuffer().appendBuffer(buffer))
    if (logger.isDebugEnabled) {
      logger.debug("<<<< onStatus")
    }
  }

  override fun onCommand(msg: Message) {
    val cmd = msg.toModel()
    when (cmd) {
      is CommandConnect -> {
        this.namespace = (cmd.getCmdObj() as ASObject)["app"] as String
        if (this.namespaces.exists(this.namespace)) {
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
          payload = CommandResult(1, arrayOf(CONNECTION_RESPONSE_CMD, CONNECTION_RESPONSE_INFO))
          val cmdBuffer = payload.toBuffer()
          header.fmt = FMT.F0
          header.csid = 3
          header.type = MessageType.COMMAND_AMF0
          header.length = cmdBuffer.length()
          b.appendBuffer(header.toBuffer()).appendBuffer(cmdBuffer)
          this.endpoint.write(b)
        } else {
          //TODO invalid app
          this.close()
        }
      }
      is CommandReleaseStream -> {
        this.streamKey = cmd.getStreamKey()
        val addresses = this.namespaces.address(this.namespace, streamKey)
        if (addresses.isEmpty()) {
          logger.error("cannot find any RTMP stream address!")
          this.close()
        } else {
          addresses.forEach {
            this.establish(it)
          }
          val payload = CommandResult(cmd.transId, arrayOf(null, 1))
          val payloadBuffer = payload.toBuffer()
          val header = Header(FMT.F1, 3, 0L, 0L, MessageType.COMMAND_AMF0, payloadBuffer.length())
          this.endpoint.write(header.toBuffer().appendBuffer(payloadBuffer))
        }
      }
      is CommandFCPublish -> {
        val streamKey = cmd.getStreamKey()
        val infoObj = mapOf(
            "code" to "NetStream.Publish.Start",
            "description" to streamKey,
            "details" to streamKey,
            "clientid" to "0"
        )
        val payload = CommandOnFCPublish(0, arrayOf(null, infoObj))
        val buffer = payload.toBuffer()
        val header = Header(FMT.F1, 3, 0L, 0L, MessageType.COMMAND_AMF0, buffer.length())
        this.endpoint.write(header.toBuffer().appendBuffer(buffer))
      }
      is CommandCreateStream -> {
        val payload = CommandResult(cmd.transId, arrayOf(null, 1))
        val payloadBuffer = payload.toBuffer()
        val header = Header(FMT.F1, 3, 0L, 0L, MessageType.COMMAND_AMF0, payloadBuffer.length())
        this.endpoint.write(header.toBuffer().appendBuffer(payloadBuffer))
      }
      is CommandCheckBW -> {
        val payload = CommandResult(cmd.transId, arrayOf(null, 1))
        val payloadBuffer = payload.toBuffer()
        val header = Header(FMT.F1, 3, 0L, 0L, MessageType.COMMAND_AMF0, payloadBuffer.length())
        this.endpoint.write(header.toBuffer().appendBuffer(payloadBuffer))
      }
      is CommandPublish -> {
        logger.info("**** waiting for adaptors connect... ****")
      }
      else -> {
        //TODO process other commands.
        logger.info("other commans: {}", cmd)
      }
    }
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
  }

}