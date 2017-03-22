package `as`.leap.raptor.core.model.payload

class CommandDeleteStream(transId: Int, objects: List<Any>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "deleteStream"
  }
}