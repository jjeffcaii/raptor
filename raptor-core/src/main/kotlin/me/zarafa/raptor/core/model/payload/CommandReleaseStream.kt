package me.zarafa.raptor.core.model.payload

import com.google.common.base.Preconditions
import org.apache.commons.lang3.StringUtils

class CommandReleaseStream(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  fun getStreamKey(): String {
    val foo = this.values[1]
    return if (foo == null) {
      StringUtils.EMPTY
    } else {
      Preconditions.checkArgument(foo is String, "Not valid stream key type: ${foo::class}.")
      foo as String
    }
  }

  companion object {
    const val NAME = "releaseStream"
  }

}