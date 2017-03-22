package `as`.leap.raptor.core.model.payload

class CommandError(transId: Int, objects: List<Any>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "_error"
  }

}