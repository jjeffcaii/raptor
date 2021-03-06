package me.zarafa.raptor.core.model.payload

import me.zarafa.raptor.core.model.Payload
import me.zarafa.raptor.core.utils.Codecs
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
    return Buffer.buffer(Codecs.encodeAMF0(arr))
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
        .add("cmd", this.cmd)
        .add("transId", this.transId)
        .add("others", this.values)
        .omitNullValues()
        .toString()
  }

}