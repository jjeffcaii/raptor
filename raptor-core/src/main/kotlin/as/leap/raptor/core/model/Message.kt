package `as`.leap.raptor.core.model

import `as`.leap.raptor.core.utils.Buffered

abstract class Message(val header: Header) : Buffered {

  abstract fun toModel(): Payload

  abstract fun toChunks(chunkSize: Int): Array<Chunk>

}