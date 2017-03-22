package `as`.leap.raptor.core.model.payload

class CommandCheckBW(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "_checkbw"
  }

}