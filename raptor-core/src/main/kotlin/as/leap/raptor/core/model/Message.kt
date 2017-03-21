package `as`.leap.raptor.core.model

import `as`.leap.raptor.core.model.msg.Payload

abstract class Message(private val header: Header) : Buffered {

  abstract fun toModel(): Payload

  abstract fun toChunks(chunkSize: Int): Array<Chunk>

}