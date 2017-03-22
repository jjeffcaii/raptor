package `as`.leap.raptor.core.model.payload

class CommandFCPublish(transId: Int, values: Array<Any?>) : AbstractCommand(NAME, transId, values) {

  fun getStreamKey(): String {
    return this.values[1] as String
  }

  companion object {
    val NAME = "FCPublish"
  }

}