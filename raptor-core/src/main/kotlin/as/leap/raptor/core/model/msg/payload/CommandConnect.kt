package `as`.leap.raptor.core.model.msg.payload

import `as`.leap.raptor.core.model.ChunkType
import com.google.common.base.MoreObjects

class CommandConnect(type: ChunkType, transId: Int, cmdObj: Any?) : Command(type, "connect", transId, cmdObj) {

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("transId", this.transId)
        .add("cmdObj", this.cmdObj)
        .omitNullValues()
        .toString()
  }
}