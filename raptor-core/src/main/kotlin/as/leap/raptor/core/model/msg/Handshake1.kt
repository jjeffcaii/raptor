package `as`.leap.raptor.core.model.msg

import io.vertx.core.buffer.Buffer

data class Handshake1(val time: Int, val time2: Int = 0, val random: Buffer)