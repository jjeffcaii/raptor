package `as`.leap.raptor.core.model.msg

import `as`.leap.raptor.core.model.*
import io.vertx.core.buffer.Buffer

class Chunk(private val buffer: Buffer, private val header: Header) : Message<Chunk.Body> {

  override fun toBuffer(): Buffer {
    return this.buffer
  }

  override fun type(): MessageType {
    return MessageType.CHUNK
  }

  private val model: Body by lazy {
    var skip = 0
    if (this.header.csid < 64) {
      skip += 1
    } else if (this.header.csid < 320) {
      skip += 2
    } else {
      skip += 3
    }
    when (this.header.fmt) {
      FMT.F1 -> skip += 11
      FMT.F2 -> skip += 7
      FMT.F3 -> skip += 3
      else -> {
        // nothing to do.
      }
    }
    if (this.header.timestamp > 16777215) {
      skip += 4
    }

    val b = this.buffer.slice(skip, this.buffer.length())


    when (this.header.type) {
      ChunkType.CTRL_SET_CHUNK_SIZE -> {

      }
    }

    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.


  }

  override fun toModel(): Body {
    return this.model
  }

  inner class Body(header: Header) {

  }

}