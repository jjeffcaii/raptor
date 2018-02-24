package me.zarafa.raptor.core.model.payload

class CommandError(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    const val NAME = "_error"
  }

}