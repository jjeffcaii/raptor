package `as`.leap.raptor.api

import java.util.regex.Pattern

data class Address(val host: String, val context: String, val key: String, val port: Int = 1935) {

  fun toBaseURL(): String {
    return when (this.port) {
      DEFAULT_PORT -> "rtmp://$host/$context"
      else -> "rtmp://$host:$port/$context"
    }
  }

  override fun toString(): String {
    return when (this.port) {
      DEFAULT_PORT -> "rtmp://$host/$context$key"
      else -> "rtmp://$host:$port/$context$key"
    }
  }

  companion object {

    val DEFAULT_PORT = 1935

    private val PATTERN_RTMP_URL = Pattern.compile("rtmp://([a-zA-Z0-9\\-_.]+)(:[1-9][0-9]+)?/([a-zA-Z0-9_\\-]+)([/?].+)$")
    private val PATTERN_FULL = Pattern.compile("([a-zA-Z0-9_\\-]+)([/?].+)$")

    fun from(url: String): Address? {
      val matcher = PATTERN_RTMP_URL.matcher(url)
      if (!matcher.matches()) {
        return null
      }
      val host = matcher.group(1)
      val port: String? = matcher.group(2)?.substring(1)
      val context = matcher.group(3)
      val key = matcher.group(4)
      if (port == null) {
        return Address(host, context, key)
      } else {
        return Address(host, context, key, port.toInt())
      }
    }

    fun extractFull(str: String): Pair<String, String>? {
      val mat = PATTERN_FULL.matcher(str)
      return if (mat.find()) {
        Pair(mat.group(1), mat.group(2))
      } else {
        null
      }
    }
  }


}