package `as`.leap.raptor.core.model.payload

class CommandConnect(transId: Int, objects: List<Any>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "connect"
  }

}