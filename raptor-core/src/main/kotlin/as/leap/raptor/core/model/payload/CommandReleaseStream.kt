package `as`.leap.raptor.core.model.payload

class CommandReleaseStream(transId: Int, objects: List<Any>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    val NAME = "releaseStream"
  }

}