package `as`.leap.raptor.core

import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.payload.*
import `as`.leap.raptor.core.utils.VertxHelper
import com.google.common.base.Preconditions
import flex.messaging.io.amf.ASObject
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.*

class Swapper {

  private var chunkSize: Long = 128
  private val front: Endpoint
  private val income: MessageFliper
  private var hashForS1: Int? = null

  private val namespaces: NamespaceManager = NamespaceManager.INSTANCE

  constructor(front: Endpoint) {
    this.front = front
    this.income = MessageFliper()
    this.income.onMessage {
      when (it.header.type) {
        ChunkType.CTRL_SET_CHUNK_SIZE -> {
          this.chunkSize = (it.toModel() as ProtocolChunkSize).chunkSize
        }
        ChunkType.COMMAND_AMF3, ChunkType.COMMAND_AMF0 -> {
          this.handleFrontCommand(it)
        }
        else -> {
          //TODO
        }
      }
    }
    this.front
        .onHandshake(this::handshakeWithFront)
        .onChunk {
          this.income.append(it)
        }
  }


  /**
   * process frontend command message.
   */
  private fun handleFrontCommand(msg: Message) {
    val cmd = msg.toModel()
    when (cmd) {
      is CommandConnect -> {
        logger.info("rcv connect: {}", cmd)
        val app = (cmd.getCmdObj() as ASObject)["app"] as String
        if (this.namespaces.exists(app)) {
          // send ack window size
          val b = Buffer.buffer()
          var header: Header = Header.getProtocolHeader(ChunkType.CTRL_ACK_WINDOW_SIZE)
          var payload: Payload = ProtocolAckWindowSize()
          b.appendBuffer(header.toBuffer()).appendBuffer(payload.toBuffer())
          // send set peer band width.
          header = Header.getProtocolHeader(ChunkType.CTRL_SET_PEER_BANDWIDTH, 5)
          payload = ProtocolBandWidth(limit = 2)
          b.appendBuffer(header.toBuffer()).appendBuffer(payload.toBuffer())
          // send set chunk size 1024
          header = Header.getProtocolHeader(ChunkType.CTRL_SET_CHUNK_SIZE)
          payload = ProtocolChunkSize(SND_CHUNK_SIZE)
          b.appendBuffer(header.toBuffer()).appendBuffer(payload.toBuffer())
          // send _result
          val cmdObj = mapOf(
              "fmsVer" to "FMS/3,0,1,123",
              "capabilities" to 31
          )
          val cmdInfo = mapOf(
              "level" to "status",
              "code" to "NetConnection.Connect.Success",
              "description" to "Connection successed.",
              "objectEncoding" to 0
          )
          payload = CommandResult(1, arrayOf(cmdObj, cmdInfo))
          val cmdBuffer = payload.toBuffer()
          header = Header(FMT.F0, 3, 0L, 0L, ChunkType.COMMAND_AMF0, cmdBuffer.length())
          b.appendBuffer(header.toBuffer()).appendBuffer(cmdBuffer)
          this.rcv(b)
        } else {
          //TODO invalid app
          this.disconnect()
        }
      }
      is CommandReleaseStream -> {
        val payload = CommandResult(cmd.transId, arrayOfNulls<Any>(1))
        val payloadBuffer = payload.toBuffer()
        val header = Header(FMT.F1, 3, 0L, 0L, ChunkType.COMMAND_AMF0, payloadBuffer.length())
        this.rcv(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(payloadBuffer))
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
        val header = Header(FMT.F1, 3, 0L, 0L, ChunkType.COMMAND_AMF0, buffer.length())
        this.rcv(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(buffer))
      }
      is CommandCreateStream -> {
        val payload = CommandResult(cmd.transId, arrayOfNulls<Any>(1))
        val payloadBuffer = payload.toBuffer()
        val header = Header(FMT.F1, 3, 0L, 0L, ChunkType.COMMAND_AMF0, payloadBuffer.length())
        this.rcv(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(payloadBuffer))
      }
      is CommandCheckBW -> {
        val payload = CommandResult(cmd.transId, arrayOfNulls<Any>(1))
        val payloadBuffer = payload.toBuffer()
        val header = Header(FMT.F1, 3, 0L, 0L, ChunkType.COMMAND_AMF0, payloadBuffer.length())
        this.rcv(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(payloadBuffer))
      }
      is CommandPublish -> {
        val infoObj = mapOf(
            "level" to "status",
            "code" to "NetStream.Publish.Start",
            "description" to "Start Publishing",
            "objectEncoding" to 0
        )
        val payload = CommandOnStatus(cmd.transId, arrayOf(null, infoObj))
        val b = payload.toBuffer()
        val header = Header(FMT.F0, msg.header.csid, 0, msg.header.streamId, ChunkType.COMMAND_AMF0, b.length())
        this.rcv(Buffer.buffer().appendBuffer(header.toBuffer()).appendBuffer(b))
      }
      else -> {
        //TODO process other commands.
      }
    }
  }

  private fun disconnect() {
    this.front.close()
  }

  private fun handshakeWithFront(handshake: Handshake) {
    val hs = handshake.toModel()
    when (hs) {
      is Handshake.C0 -> {
        val c0 = Handshake.C0()
        Preconditions.checkArgument(c0.version == 3.toShort(), "Not valid RTMP version: %d.", c0.version)
      }
      is Handshake.C12 -> {
        when (hs.v2) {
          0L -> {
            val s0 = Handshake.C0()
            val random = Buffer.buffer().appendString(UUID.randomUUID().toString())
            // fill with zero
            VertxHelper.fillZero(random, 1528)
            val s1 = Handshake.C12(System.currentTimeMillis() / 1000, hs.v1, random)
            val b = Buffer.buffer(3073)
                .appendBuffer(s0.toBuffer())
                .appendBuffer(s1.toBuffer())
                .appendBuffer(handshake.toBuffer())
            this.hashForS1 = s1.hash()
            this.rcv(b)
          }
          else -> {
            Preconditions.checkArgument(hs.hash() == this.hashForS1, "Not valid handshake: C2 <> S1.")
            this.hashForS1 = null
            if (logger.isDebugEnabled) {
              logger.debug("handshake with front success!")
            }
          }
        }
      }
    }
  }

  private fun rcv(buffer: Buffer) {
    this.front.write(buffer)
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private val SND_CHUNK_SIZE = 1024L
  }

}