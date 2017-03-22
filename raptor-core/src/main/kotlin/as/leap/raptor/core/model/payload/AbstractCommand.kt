package `as`.leap.raptor.core.model.payload

import `as`.leap.raptor.core.model.Payload
import `as`.leap.raptor.core.utils.CodecHelper
import com.google.common.base.MoreObjects
import io.vertx.core.buffer.Buffer

abstract class AbstractCommand(val cmd: String, val transId: Int, protected val values: List<Any>) : Payload {

  override fun toBuffer(): Buffer {
    val li = mutableListOf(cmd, transId)
    li.addAll(values)
    return Buffer.buffer(CodecHelper.encodeAMF0(li))
  }

  open fun getCmdObj(): Any? {
    val obj = this.values[0]
    return when (obj) {
      is Unit -> null
      else -> obj
    }
  }

  open fun getInfo(): Any? {
    return null
  }

  protected fun toStringHelper(): MoreObjects.ToStringHelper {
    return MoreObjects.toStringHelper(this)
        .add("cmd", this.cmd)
        .add("transId", this.transId)
        .add("others", this.values)
  }

  override fun toString(): String {
    return this.toStringHelper()
        .omitNullValues()
        .toString()
  }

}