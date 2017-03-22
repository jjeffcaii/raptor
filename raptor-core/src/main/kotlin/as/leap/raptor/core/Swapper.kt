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
          payload = ProtocolChunkSize(1024)
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
          payload = CommandResult(1, listOf(cmdObj, cmdInfo))
          val cmdBuffer = payload.toBuffer()
          header = Header(FMT.F0, 3, 0L, 0L, ChunkType.COMMAND_AMF0, cmdBuffer.length())
          b.appendBuffer(header.toBuffer()).appendBuffer(cmdBuffer)

          this.rcv(b)
        } else {
          //TODO
        }
      }
      else -> {

      }
    }
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

  }

}