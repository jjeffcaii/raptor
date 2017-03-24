package `as`.leap.raptor.core.ext

import `as`.leap.raptor.core.model.Handshake
import `as`.leap.raptor.core.utils.Do
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.*

class Handshaker(private val endpoint: Endpoint,
                 val success: Do? = null,
                 val failed: Do? = null,
                 private val passive: Boolean = false) {

  private var hash: Triple<Long, Long, String>? = null

  private var step = 0

  init {
    if (!this.passive) {
      val c0 = Handshake.C0_INSTANCE
      val random = makeRandom(1528)
      val c1 = Handshake.C12(System.currentTimeMillis() / 1000, 0, random)
      val b = Buffer.buffer(1537)
          .appendBuffer(c0.toBuffer())
          .appendBuffer(c1.toBuffer())
      this.hash = c1.hash()
      this.endpoint.write(b)
    }
  }

  fun validate(handshake: Handshake) {
    synchronized(this) {
      if (this.passive) {
        this.passive(handshake)
      } else {
        this.initiative(handshake)
      }
    }
    this.step++
  }

  // 主动模式
  // YOU -- C* -> SERVER
  // YOU <- S* -- SERVER
  private fun initiative(handshake: Handshake) {
    val sx = handshake.toModel()
    when (this.step) {
      0 -> {

        if (sx !is Handshake.C0) {
          logger.error("invalid handshake: illegal S0 type {}.", sx::class)
          this.failed?.invoke()
        } else if (sx.version != Handshake.C0_INSTANCE.version) {
          logger.error("invalid handshake: illegal S0 version {}.", sx.version)
          this.failed?.invoke()
        }
      }
      1 -> {
        if (sx !is Handshake.C12) {
          logger.error("invalid handshake: illegal type {}.", sx::class)
          this.failed?.invoke()
        } else {
          this.endpoint.write(handshake.toBuffer())
        }
      }
      2 -> {
        if (sx !is Handshake.C12) {
          logger.error("invalid handshake: illegal type {}.", sx::class)
          this.failed?.invoke()
        } else if (sx.hash() != this.hash) {
          logger.error("invalid handshake: C1(cached)={}, S2(recieved)={}", this.hash, sx.hash())
          this.failed?.invoke()
        } else {
          this.success?.invoke()
        }
      }
      else -> {
        throw UnsupportedOperationException("Not valid handshake step: ${this.step}.")
      }
    }
  }

  // 被动模式
  // CLIENT -- C* -> YOU
  // CLIENT <- S* -- YOU
  private fun passive(handshake: Handshake) {
    val cx = handshake.toModel()
    when (this.step) {
      0 -> {
        if (cx !is Handshake.C0) {
          logger.error("invalid handshake: illegal C0 type {}.", cx::class)
          this.failed?.invoke()
        } else if (cx.version != Handshake.C0_INSTANCE.version) {
          logger.error("invalid handshake: illegal C0 version {}.", cx.version)
          this.failed?.invoke()
        }
      }
      1 -> {
        // recieve c1, send s0+s1+s2
        if (cx !is Handshake.C12) {
          logger.error("invalid handshake: illegal C1 type {}.", cx::class)
          this.failed?.invoke()
        } else {
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
      }
      2 -> {
        // recieve c2
        if (cx !is Handshake.C12) {
          logger.error("invalid handshake: illegal C2 type {}.", cx::class)
          this.failed?.invoke()
        } else if (cx.hash() != this.hash) {
          logger.error("invalid handshake: C2(recieved)={}, S1(cached)={}.", cx.hash(), this.hash!!)
          this.failed?.invoke()
        } else {
          this.hash = null
          if (logger.isDebugEnabled) {
            logger.debug("handshake with front success!")
          }
          this.success?.invoke()
        }
      }
      else -> {
        throw UnsupportedOperationException("Not valid handshake step: ${this.step}.")
      }
    }

    when (cx) {
      is Handshake.C0 -> {
        if (cx.version != Handshake.C0_INSTANCE.version) {
          logger.error("invalid handshake: illegal C0 version {}.", cx.version)
          this.failed?.invoke()
        }
      }
      is Handshake.C12 -> {
        if (isS2OrC1(cx)) {
          val s0 = Handshake.C0_INSTANCE
          val random = makeRandom(1528)
          val s1 = Handshake.C12(System.currentTimeMillis() / 1000, cx.v1, random)
          val b = Buffer.buffer(3073)
              .appendBuffer(s0.toBuffer())
              .appendBuffer(s1.toBuffer())
              .appendBuffer(handshake.toBuffer())
          this.hash = s1.hash()
          this.endpoint.write(b)
        } else {
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


  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private fun makeRandom(len: Int): Buffer {
      val b = Buffer.buffer().appendString(UUID.randomUUID().toString())
      do {
        b.appendUnsignedByte(0)
      } while (b.length() < len)
      return b
    }

    private fun isS2OrC1(c12: Handshake.C12): Boolean {
      return c12.v1 != 0L && c12.v2 == 0L
    }

  }

}