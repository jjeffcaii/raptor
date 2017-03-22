package `as`.leap.raptor.core.model.payload

class CommandClose(transId: Int, objects: List<Any>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "close"
  }
}