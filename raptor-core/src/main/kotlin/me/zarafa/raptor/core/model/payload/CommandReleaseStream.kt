package me.zarafa.raptor.core.model.payload

import com.google.common.base.Preconditions
import org.apache.commons.lang3.StringUtils

class CommandReleaseStream(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  fun getStreamKey(): String {
    val foo = this.values[1]
    if (foo == null) {
      return StringUtils.EMPTY
    } else {
      Preconditions.checkArgument(foo is String, "Not valid stream key type: ${foo::class}.")
      return foo as String
    }
  }

  companion object {
    val NAME = "releaseStream"
  }

}