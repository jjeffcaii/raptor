package me.zarafa.raptor.server.vo

import me.zarafa.raptor.api.Address
import java.util.regex.Pattern

data class PostAddress(val url: String, val streamKey: String? = null) {

  fun toAddress(): Address? {
    if (streamKey.isNullOrBlank()) {
      return Address.from(this.url)
    }
    val matcher = PATTERN_RTMP_URL.matcher(this.url)
    if (!matcher.matches()) {
      return null
    }
    val host = matcher.group(1)
    val port: String? = matcher.group(2)?.substring(1)
    val context = matcher.group(3)
    if (port == null) {
      return Address(host, context, this.streamKey!!)
    } else {
      return Address(host, context, this.streamKey!!, port.toInt())
    }
  }

  companion object {
    private val PATTERN_RTMP_URL = Pattern.compile("rtmp://([a-zA-Z0-9\\-_.]+)(:[1-9][0-9]*+)?/(.+)")
  }

}