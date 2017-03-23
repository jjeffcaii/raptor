package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.Handshake
import `as`.leap.raptor.core.utils.Do
import `as`.leap.raptor.core.utils.VertxHelper
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.*

class HandshakeContext(
    private val endpoint: Endpoint,
    val success: Do? = null,
    val failed: Do? = null,
    private val passive: Boolean = false
) {

  private var hash: Triple<Long, Long, Long>? = null
  private var ts: Long? = null

  init {
    if (!this.passive) {
      val c0 = Handshake.C0()
      val random = makeRandom(1528)
      val t = System.currentTimeMillis() / 1000
      this.ts = t
      val c1 = Handshake.C12(t, 0, random)
      val b = Buffer.buffer()
          .appendBuffer(c0.toBuffer())
          .appendBuffer(c1.toBuffer())
      this.hash = c1.hash()
      this.endpoint.write(b)
    }
  }

  // 主动模式
  private fun initiative(handshake: Handshake) {
    val hs = handshake.toModel()
    when (hs) {
      is Handshake.C0 -> {
        val s0 = Handshake.C0()
        if (s0.version != 3.toShort()) {
          logger.error("Not valid RTMP version: {}.", s0.version)
          this.failed?.invoke()
        }
      }
      is Handshake.C12 -> {
        when (hs.v2) {
          0L -> {
            // s2 = c1
            val s2Hash = hs.hash()
            if (s2Hash != this.hash) {
              logger.error("Not valid handshake: C1={}, S2={}", s2Hash, this.hash)
              this.failed?.invoke()
            } else {
              this.success?.invoke()
            }
          }
          else -> {
            // s1
            if (hs.v2 != this.ts) {
              logger.error("Not valid S1: time2={}, should={}.", hs.v2, this.ts)
              this.failed?.invoke()
            } else {
              this.endpoint.write(handshake.toBuffer())
            }
          }
        }
      }
    }
  }

  // 被动模式
  private fun passive(handshake: Handshake) {
    val hs = handshake.toModel()
    when (hs) {
      is Handshake.C0 -> {
        val c0 = Handshake.C0()
        if (c0.version != 3.toShort()) {
          logger.error("Not valid RTMP version: {}.", c0.version)
          this.failed?.invoke()
        }
      }
      is Handshake.C12 -> {
        when (hs.v2) {
          0L -> {
            val s0 = Handshake.C0()
            val random = makeRandom(1528)
            val s1 = Handshake.C12(System.currentTimeMillis() / 1000, hs.v1, random)
            val b = Buffer.buffer(3073)
                .appendBuffer(s0.toBuffer())
                .appendBuffer(s1.toBuffer())
                .appendBuffer(handshake.toBuffer())
            this.hash = s1.hash()
            this.endpoint.write(b)
          }
          else -> {
            val hash2 = hs.hash()
            if (hash2 != this.hash) {
              logger.error("Not valid handshake: C2={}, S1={}", hash2, this.hash)
              this.failed?.invoke()
            } else {
              this.hash = null
              if (logger.isDebugEnabled) {
                logger.debug("handshake with front success!")
              }
              this.success?.invoke()
            }
          }
        }
      }
    }
  }

  fun check(handshake: Handshake) {
    if (this.passive) {
      this.passive(handshake)
    } else {
      this.initiative(handshake)
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private fun makeRandom(len: Int): Buffer {
      val b = Buffer.buffer().appendString(UUID.randomUUID().toString())
      VertxHelper.fillZero(b, len)
      return b
    }
  }

}