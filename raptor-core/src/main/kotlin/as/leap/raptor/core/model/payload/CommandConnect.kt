package `as`.leap.raptor.core.model.payload

class CommandConnect(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "connect"
  }

}