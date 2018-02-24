package me.zarafa.raptor.core.model.payload

class CommandOnFCPublish(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  companion object {
    const val NAME = "onFCPublish"
  }

}