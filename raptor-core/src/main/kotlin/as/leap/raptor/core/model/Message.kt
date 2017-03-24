package `as`.leap.raptor.core.model

import `as`.leap.raptor.core.utils.Buffered
import io.vertx.core.buffer.Buffer

abstract class Message(val header: Header) : Buffered {

  abstract fun toModel(): Payload

  abstract fun toChunks(chunkSize: Long): List<Chunk>

  abstract fun toBuffer(chunkSize: Long): Buffer


}