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

  init {
    if (!this.passive) {
      val c0 = Handshake.C0()
      val random = makeRandom(1528)
      val c1 = Handshake.C12(System.currentTimeMillis() / 1000, 0, random)
      val b = Buffer.buffer()
          .appendBuffer(c0.toBuffer())
          .appendBuffer(c1.toBuffer())
      this.hash = c1.hash()
      this.endpoint.write(b)
    }
  }

  // 主动模式
  // YOU -- C* -> SERVER
  // YOU <- S* -- SERVER
  private fun initiative(handshake: Handshake) {
    val sx = handshake.toModel()
    when (sx) {
      is Handshake.C0 -> {
        if (sx.version != Handshake.C0_INSTANCE.version) {
          logger.error("invalid handshake: illegal S0 version {}.", sx.version)
          this.failed?.invoke()
        }
      }
      is Handshake.C12 -> {
        when (sx.v2) {
          0L -> {
            // recieve s2, validate s2=c1
            val hashForS2 = sx.hash()
            if (hashForS2 != this.hash) {
              logger.error("invalid handshake: C1(cached)={}, S2(recieved)={}", this.hash, hashForS2)
              this.failed?.invoke()
            } else {
              this.success?.invoke()
            }
          }
          else -> {
            // recieve s1, validate s1.time2 == c1.time
            if (sx.v2 != this.hash!!.first) {
              logger.error("Not valid S1: time2={}, should={}.", sx.v2, this.hash!!.first)
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
  // CLIENT -- C* -> YOU
  // CLIENT <- S* -- YOU
  private fun passive(handshake: Handshake) {
    val cx = handshake.toModel()
    when (cx) {
      is Handshake.C0 -> {
        if (cx.version != Handshake.C0_INSTANCE.version) {
          logger.error("invalid handshake: illegal C0 version {}.", cx.version)
          this.failed?.invoke()
        }
      }
      is Handshake.C12 -> {
        when (cx.v2) {
          0L -> {
            // recieve c1, send s0+s1+s2
            val s0 = Handshake.C0_INSTANCE
            val random = makeRandom(1528)
            val s1 = Handshake.C12(System.currentTimeMillis() / 1000, cx.v1, random)
            val b = Buffer.buffer(3073)
                .appendBuffer(s0.toBuffer())
                .appendBuffer(s1.toBuffer())
                .appendBuffer(handshake.toBuffer())
            this.hash = s1.hash()
            this.endpoint.write(b)
          }
          else -> {
            // recieve c2
            val hashForC2 = cx.hash()
            if (hashForC2 != this.hash!!) {
              logger.error("invalid handshake: C2(recieved)={}, S1(cached)={}.", hashForC2, this.hash!!)
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
    synchronized(this) {
      if (this.passive) {
        this.passive(handshake)
      } else {
        this.initiative(handshake)
      }
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