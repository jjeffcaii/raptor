package me.zarafa.raptor.core.model.payload

class CommandCheckBW(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    const val NAME = "_checkbw"
  }

}