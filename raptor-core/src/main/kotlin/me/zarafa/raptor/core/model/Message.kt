package me.zarafa.raptor.core.model

import me.zarafa.raptor.core.utils.Buffered
import io.vertx.core.buffer.Buffer

abstract class Message(val header: Header) : Buffered {

  abstract fun toModel(): Payload

  abstract fun toChunks(chunkSize: Long): List<Chunk>

  abstract fun toBuffer(chunkSize: Long): Buffer


}