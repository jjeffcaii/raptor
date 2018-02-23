package `in`.firedog.raptor.core.model.payload

class CommandDeleteStream(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "deleteStream"
  }
}