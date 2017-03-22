package `as`.leap.raptor.core.utils

import `as`.leap.raptor.core.model.payload.ProtocolWindowSize
import io.vertx.core.buffer.Buffer

object Buffers {

  val winAckSize: Buffer by lazy {
    val b = Buffer.buffer()
    val p = ProtocolWindowSize()

    b
  }

}