package `as`.leap.raptor.core.model.payload

class CommandReleaseStream(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  fun getStreamId(): String {
    return this.values[1] as String
  }

  companion object {
    val NAME = "releaseStream"
  }

}