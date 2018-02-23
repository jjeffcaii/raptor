package `in`.firedog.raptor.api.exception

open class RaptorException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {

  protected var code1: Int = 5000
  protected var code2: Int = 500

  fun code(code: Int? = null, httpCode: Int? = null): RaptorException {
    code?.let {
      this.code1 = it
    }
    httpCode?.let {
      this.code2 = it
    }
    return this
  }

  fun toCode(): Pair<Int, Int> {
    return Pair(this.code1, this.code2)
  }

}