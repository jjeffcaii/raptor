package me.zarafa.raptor.core.model.payload

class CommandFCPublish(transId: Int, values: Array<Any?>) : AbstractCommand(NAME, transId, values) {

  fun getStreamKey(): String {
    return this.values[1] as String
  }

  companion object {
    const val NAME = "FCPublish"
  }

}