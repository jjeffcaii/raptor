package `as`.leap.raptor.core.model.payload

class CommandFCPublish(transId: Int, objects: List<Any>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "FCPublish"

  }

}