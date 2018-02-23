package `in`.firedog.raptor.core.model.payload

class CommandPublish(transId: Int, values: Array<Any?>) : AbstractCommand(NAME, transId, values) {

  companion object {
    val NAME = "publish"
  }

}