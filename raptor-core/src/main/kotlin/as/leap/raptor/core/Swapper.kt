package `as`.leap.raptor.core

import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.core.model.ChunkType
import `as`.leap.raptor.core.model.Handshake
import `as`.leap.raptor.core.model.Message
import `as`.leap.raptor.core.model.SimpleMessage
import `as`.leap.raptor.core.model.msg.payload.CommandConnect
import `as`.leap.raptor.core.model.msg.payload.ProtocolChunkSize
import `as`.leap.raptor.core.utils.CodecHelper
import `as`.leap.raptor.core.utils.VertxHelper
import com.google.common.base.Preconditions
import flex.messaging.io.amf.ASObject
import io.vertx.core.buffer.Buffer
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.*

class Swapper {

  private var chunkSize: Long = 128
  private val front: Endpoint
  private val income: MessageFliper
  private var s1: Triple<Long, Long, String>? = null

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
          // send
          



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
            this.s1 = Triple(s1.v1, s1.v2, CodecHelper.murmur32(random.bytes))
            this.rcv(b)
          }
          else -> {
            val actual = this.s1!!
            Preconditions.checkArgument(hs.v1 == actual.first, "Not valid handshake: C2.time=${hs.v1}, S1.time=${actual.first}.")
            Preconditions.checkArgument(hs.v2 == actual.second, "Not valid handshake: C2.time2=${hs.v2}, S1.time2=${actual.second}.")
            val c2random = CodecHelper.murmur32(hs.random.bytes)
            Preconditions.checkArgument(StringUtils.equals(c2random, actual.third), "Not valid handshake: C2.random=$c2random, S1.random=${actual.third}.")
            if (logger.isDebugEnabled) {
              logger.debug("handshake with front success!")
            }
            this.s1 = null
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