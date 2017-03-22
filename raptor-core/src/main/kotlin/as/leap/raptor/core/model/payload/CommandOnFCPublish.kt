package `as`.leap.raptor.core.model.payload

class CommandOnFCPublish(transId: Int, objects: List<Any>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "onFCPublish"
  }

}