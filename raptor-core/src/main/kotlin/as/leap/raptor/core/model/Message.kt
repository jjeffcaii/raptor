package `as`.leap.raptor.core.model

abstract class Message(val header: Header) : Buffered {

  abstract fun toModel(): Payload

  abstract fun toChunks(chunkSize: Int): Array<Chunk>

}