package me.zarafa.raptor.core.model.payload

class CommandPublish(transId: Int, values: Array<Any?>) : AbstractCommand(NAME, transId, values) {

  companion object {
    const val NAME = "publish"
  }

}