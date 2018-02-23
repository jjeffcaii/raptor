package `in`.firedog.raptor.server.vo

import `in`.firedog.raptor.commons.Errors
import `in`.firedog.raptor.commons.Utils

data class ErrorMessage(val code: Int = Errors.unknown, val msg: String? = null) {

  override fun toString(): String {
    return Utils.toJSON(this)
  }

}