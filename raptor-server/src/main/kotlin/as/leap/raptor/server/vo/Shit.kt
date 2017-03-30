package `as`.leap.raptor.server.vo

import `as`.leap.raptor.commons.Errors
import `as`.leap.raptor.commons.Utils

data class Shit(val code: Int = Errors.unknown, val msg: String? = null) {

  override fun toString(): String {
    return Utils.toJSON(this)
  }

}