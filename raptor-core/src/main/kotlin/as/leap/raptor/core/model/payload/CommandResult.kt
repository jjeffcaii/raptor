package `as`.leap.raptor.core.model.payload

class CommandResult(transId: Int, values: Array<Any?>) : AbstractCommand(NAME, transId, values) {

  companion object {
    val NAME = "_result"
  }

}