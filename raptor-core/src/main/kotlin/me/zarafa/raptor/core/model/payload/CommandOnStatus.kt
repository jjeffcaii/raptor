package me.zarafa.raptor.core.model.payload

class CommandOnStatus(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    const val NAME = "onStatus"
  }
}