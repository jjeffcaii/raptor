package `as`.leap.raptor.core.model.payload

class CommandCreateStream(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "createStream"
  }
}