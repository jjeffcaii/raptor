package `as`.leap.raptor.api

data class Address(val host: String, val context: String, val key: String, val port: Int = 1935, val provider: Address.Provider = Address.Provider.DEFAULT) {

  private val base: String by lazy {
    when (this.port) {
      1935 -> "rtmp://$host/$context"
      else -> "rtmp://$host:$port/$context"
    }
  }

  fun toBaseURL(): String {
    return this.base
  }

  enum class Provider {
    DEFAULT, QINIU, BILIBILI, DOUYU
  }

  override fun toString(): String {
    return when (this.port) {
      1935 -> "rtmp://$host/$context$key"
      else -> "rtmp://$host:$port/$context$key"
    }
  }

}