package `in`.firedog.raptor.core.model.payload

class CommandClose(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "close"
  }
}