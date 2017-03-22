package `as`.leap.raptor.core.model.payload

class CommandPublish(transId: Int, objects: List<Any>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "publish"
  }

}