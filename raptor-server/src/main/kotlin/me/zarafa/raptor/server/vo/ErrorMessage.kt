package me.zarafa.raptor.server.vo

import me.zarafa.raptor.commons.Errors
import me.zarafa.raptor.commons.Utils

data class ErrorMessage(val code: Int = Errors.unknown, val msg: String? = null) {

  override fun toString(): String {
    return Utils.toJSON(this)
  }

}