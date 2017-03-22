package `as`.leap.raptor.core.model.payload

class CommandOnStatus(transId: Int, objects: List<Any>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "onStatus"
  }
}