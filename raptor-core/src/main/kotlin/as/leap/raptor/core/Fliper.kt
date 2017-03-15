package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.Message
import io.vertx.core.buffer.Buffer

class Fliper {

  private val datum: Datum
  private val reader: Reader
  private val handler: (List<Message>) -> Unit

  constructor(handler: (List<Message>) -> Unit, alloc: Int = 1048576) {
    this.datum = SimpleDatum(alloc)
    this.reader = Reader(this.datum)
    this.handler = handler
  }

  fun write(buffer: Buffer): Fliper {
    this.datum.write(buffer)
    this.read(mutableListOf<Message>())
    return this
  }

  private fun read(results: MutableList<Message>) {
    var result: Message? = this.reader.next()
    while (result != null) {
      results.add(result)
      result = this.reader.next()
    }
    if (results.isNotEmpty()) {
    }
  }

  private enum class ReaderState {
    WAITING,
    IDLE,
    HANDSHAKE0,
    HANDSHAKE1,
    HANDSHAKE2,
    CHUNK_HEADER,
    CHUNK_BODY,
    CHUNK_BODY_PROTOCOL_CONTROL,
    CHUNK_BODY_OTHER
  }

  private inner class Reader(val datum: Datum) {

    private var prevState: ReaderState = ReaderState.HANDSHAKE0
    private var state: ReaderState = ReaderState.WAITING

    fun next(): Message? {
      var ret: Message? = null
      when (this.state) {
        else -> {
        }
      }
      return ret
    }

  }


}



