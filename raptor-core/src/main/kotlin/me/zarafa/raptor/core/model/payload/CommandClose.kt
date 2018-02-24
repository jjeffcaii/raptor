package me.zarafa.raptor.core.model.payload

class CommandClose(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    const val NAME = "close"
  }
}