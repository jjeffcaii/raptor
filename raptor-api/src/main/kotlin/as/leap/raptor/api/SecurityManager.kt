package `as`.leap.raptor.api

interface SecurityManager {

  fun exists(namespace: String): Boolean

  fun validate(namespace: String, streamKey: String): Result

  class Result(val success: Boolean, val group: String)

  companion object {
    val GOD_KEY = "iseedeadpeople"
  }

}