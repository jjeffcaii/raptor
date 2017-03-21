package `as`.leap.raptor.core.model.msg.payload

import `as`.leap.raptor.core.model.msg.Payload
import com.google.common.base.MoreObjects

abstract class Command(protected val objects: List<Any>) : Payload {

  fun getCmd(): String {
    return this.objects[0] as String
  }

  fun getTransId(): Int {
    return (this.objects[1] as Number).toInt()
  }

  open fun getCmdObj(): Any? {
    val obj = this.objects[2]
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
        .add("cmd", this.getCmd())
        .add("transId", this.getTransId())
        .add("cmdObj", this.getCmdObj())
        .add("info", this.getInfo())
  }

  override fun toString(): String {
    return this.toStringHelper()
        .omitNullValues()
        .toString()
  }

}