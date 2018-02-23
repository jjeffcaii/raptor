package `in`.firedog.raptor.api

interface SecurityManager {

  fun exists(namespace: String): Boolean

  fun validate(namespace: String, streamKey: String): Result

  fun nativeValidate(namespace: String, clientKey: String): Boolean

  class Result(val success: Boolean, val group: String)

}