package `as`.leap.raptor.core

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.core.adaptor.QiniuAdaptor
import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.payload.*
import flex.messaging.io.amf.ASObject
import io.vertx.core.buffer.Buffer
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.invoke.MethodHandles

class Swapper : Closeable {

  private var chunkSize: Long = 128
  private var namespace: String = StringUtils.EMPTY
  private var streamKey: String = StringUtils.EMPTY
  private val front: Endpoint
  private val namespaces: NamespaceManager = NamespaceManager.INSTANCE
  private val adaptors: MutableList<Adaptor> = mutableListOf()

  constructor(front: Endpoint) {
    this.front = front
    val messages = MessageFliper()
    messages.onMessage { msg ->
      when (msg.header.type) {
        MessageType.CTRL_SET_CHUNK_SIZE -> this.chunkSize = (msg.toModel() as ProtocolChunkSize).chunkSize
        MessageType.COMMAND_AMF3, MessageType.COMMAND_AMF0 -> this.handleFrontCommand(msg)
        MessageType.DATA_AMF0, MessageType.DATA_AMF3 -> {
          val payload = msg.toModel() as SimpleAMFPayload
          when (payload.body[0]) {
            "@setDataFrame" -> {
              this.adaptors.forEach {
                it.write(msg.toBuffer())
              }
            }
          }
        }
        else -> {
          //TODO handle other message
        }
      }
    }
    val hc = HandshakeContext(this.front, failed = { this.close() }, passive = true)
    this.front
        .onHandshake(hc::check)
        .onChunk {
          when {
            it.header.type.isMedia() -> {
              val buffer = it.toBuffer()
              this.adaptors.forEach {
                it.write(buffer)
              }
            }
            else -> {
              messages.append(it)
            }
          }
        }
  }

  /**
   * process frontend command message.
   */
  private fun handleFrontCommand(msg: Message) {
    val cmd = msg.toModel()
    when (cmd) {
      is CommandConnect -> {
        this.namespace = (cmd.getCmdObj() as ASObject)["app"] as String
        if (this.namespaces.exists(this.namespace)) {
          // send ack window size
          val b = Buffer.buffer()
          var header: Header = Header.getProtocolHeader(MessageType.CTRL_ACK_WINDOW_SIZE)
          var payload: Payload = ProtocolAckWindowSize()
          b.appendBuffer(header.toBuffer()).appendBuffer(payload.toBuffer())
          // send set peer band width.
          header = Header.getProtocolHeader(MessageType.CTRL_SET_PEER_BANDWIDTH, 5)
          payload = ProtocolBandWidth(limit = 2)
          b.appendBuffer(header.toBuffer()).appendBuffer(payload.toBuffer())
          // send set chunk size 1024
          header = Header.getProtocolHeader(MessageType.CTRL_SET_CHUNK_SIZE)
          payload = ProtocolChunkSize(SND_CHUNK_SIZE)
          b.appendBuffer(header.toBuffer()).appendBuffer(payload.toBuffer())
          // send _result
          val cmdObj = mapOf(
              "fmsVer" to "FMS/3,0,1,123",
              "capabilities" to 31
          )
          val infoObj = mapOf(
              "level" to "status",
              "code" to "NetConnection.Connect.Success",
              "description" to "Connection successed.",
              "objectEncoding" to 0
          )
          payload = CommandResult(1, arrayOf(cmdObj, infoObj))
          val cmdBuffer = payload.toBuffer()
          header = Header(FMT.F0, 3, 0L, 0L, MessageType.COMMAND_AMF0, cmdBuffer.length())
          b.appendBuffer(header.toBuffer()).appendBuffer(cmdBuffer)
          this.rcv(b)
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
          this.rcv(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(payloadBuffer))
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
        this.rcv(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(buffer))
      }
      is CommandCreateStream -> {
        val payload = CommandResult(cmd.transId, arrayOf(null, 1))
        val payloadBuffer = payload.toBuffer()
        val header = Header(FMT.F1, 3, 0L, 0L, MessageType.COMMAND_AMF0, payloadBuffer.length())
        this.rcv(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(payloadBuffer))
      }
      is CommandCheckBW -> {
        val payload = CommandResult(cmd.transId, arrayOf(null, 1))
        val payloadBuffer = payload.toBuffer()
        val header = Header(FMT.F1, 3, 0L, 0L, MessageType.COMMAND_AMF0, payloadBuffer.length())
        this.rcv(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(payloadBuffer))
      }
      is CommandPublish -> {
        /*
        val infoObj = mapOf(
            "level" to "status",
            "code" to "NetStream.Publish.Start",
            "description" to "Start Publishing",
            "objectEncoding" to 0
        )
        val payload = CommandOnStatus(cmd.transId, arrayOf(null, infoObj))
        val b = payload.toBuffer()
        val header = Header(FMT.F0, msg.header.csid, 0, msg.header.streamId, MessageType.COMMAND_AMF0, b.length())
        this.rcv(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(b))
        */
        logger.info("**** waiting for adaptors connect... ****")
      }
      else -> {
        //TODO process other commands.
        logger.info("other commans: {}", cmd)
      }
    }
  }

  private fun isAllAdaptorConnected(): Boolean {
    synchronized(this.adaptors) {
      return this.adaptors.all {
        return it.connected()
      }
    }
  }

  private fun establish(address: Address) {
    val adaptor = when (address.provider) {
      Address.Provider.QINIU -> {
        QiniuAdaptor(address, this.chunkSize, {
          if (this.isAllAdaptorConnected()) {
            val buff = Buffer.buffer()
            var payload: Payload = CommandOnFCPublish(0, arrayOf(null, mapOf(
                "code" to "NetStream.Publish.Start",
                "description" to this.streamKey,
                "details" to this.streamKey,
                "clientid" to "0"
            )))
            var b: Buffer = payload.toBuffer()
            var header: Header = Header(FMT.F1, 3, 0L, 0L, MessageType.COMMAND_AMF0, b.length())
            buff.appendBuffer(header.toBuffer()).appendBuffer(b)
            val infoObj = mapOf(
                "level" to "status",
                "code" to "NetStream.Publish.Start",
                "description" to "Start Publishing",
                "objectEncoding" to 0
            )
            payload = CommandOnStatus(5, arrayOf(null, infoObj))
            b = payload.toBuffer()
            header = Header(FMT.F0, 4, 0, 0, MessageType.COMMAND_AMF0, b.length())
            buff.appendBuffer(header.toBuffer()).appendBuffer(b)
            this.rcv(buff)
            logger.info("**** start publishing! ****")
          }
        })
      }
      else -> {
        TODO("other provider adaptor")
      }
    }
    this.adaptors.add(adaptor)
  }

  private fun rcv(buffer: Buffer) {
    this.front.write(buffer)
  }

  override fun close() {
    this.front.close()
  }


  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private val SND_CHUNK_SIZE = 1024L
  }

}