package `as`.leap.raptor.core.model.payload

class CommandFCUnpublilsh(transId: Int, values: Array<Any?>) : AbstractCommand(NAME, transId, values) {

  fun getStreamKey(): String {
    return this.values[1] as String
  }

  companion object {
    val NAME = "FCUnpublish"
  }
}