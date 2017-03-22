package `as`.leap.raptor.core.model.payload

import `as`.leap.raptor.core.model.Payload
import `as`.leap.raptor.core.utils.CodecHelper
import com.google.common.base.MoreObjects
import io.vertx.core.buffer.Buffer

abstract class AbstractCommand(val cmd: String, val transId: Int, protected val values: Array<Any?>) : Payload {

  override fun toBuffer(): Buffer {
    val arr = arrayOfNulls<Any>(2 + this.values.size)
    arr[0] = this.cmd
    arr[1] = this.transId
    for (i in 0 until values.size) {
      arr[i + 2] = values[i]
    }
    return Buffer.buffer(CodecHelper.encodeAMF0(arr))
  }

  open fun getCmdObj(): Any? {
    val obj = this.values[0]
    return when (obj) {
      is Unit -> null
      else -> obj
    }
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