package `in`.firedog.raptor.core.model.payload

class CommandError(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "_error"
  }

}