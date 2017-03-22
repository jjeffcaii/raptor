package `as`.leap.raptor.core.model.payload

class CommandResult(transId: Int, objects: List<Any>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "_result"
  }

}