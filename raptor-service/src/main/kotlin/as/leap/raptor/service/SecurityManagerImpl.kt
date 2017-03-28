package `as`.leap.raptor.service

import `as`.leap.raptor.api.SecurityManager
import com.google.common.base.Splitter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.regex.Pattern

class SecurityManagerImpl(endpoint: String) : SecurityManager {

  private val base: String = "${StringUtils.strip(endpoint, "/")}/2.0/acl"

  override fun exists(namespace: String): Boolean {
    return PATTERN_NAMESPACE.matcher(namespace).matches()
  }

  override fun validate(namespace: String, streamKey: String): SecurityManager.Result {
    val sk = if (streamKey.startsWith("/") || streamKey.startsWith("?")) {
      streamKey.substring(1)
    } else {
      streamKey
    }
    val q: Map<String, String> = Splitter.on("&").withKeyValueSeparator("=").split(sk)
    val sign = q["k"]
    if (StringUtils.isBlank(sign)) {
      if (logger.isDebugEnabled) {
        logger.debug("security validate failed: token is blank.")
      }
      return FAILED
    }

    if (StringUtils.equals(sign, SecurityManager.GOD_KEY)) {
      return FAILED
    }

    val matcher = PATTERN_SIGN.matcher(sign)
    if (!matcher.matches()) {
      if (logger.isDebugEnabled) {
        logger.debug("security validate failed: sign={}.(illegal format)", sign)
      }
      return FAILED
    }

    val ts = matcher.group(1).toLong()
    val now = System.currentTimeMillis()
    if (now > ts) {
      if (logger.isDebugEnabled) {
        logger.debug("security validate failed: sign is expired.")
      }
      return FAILED
    }
    val req = Request.Builder().url(this.base)
        .header("X-ML-AppId", namespace)
        .header("X-ML-Request-Sign", sign)
        .header("Content-Type", "application/json")
        .get()
        .build()
    var success: Boolean = false
    client.newCall(req).execute().use { success = it.isSuccessful }
    return if (!success) {
      FAILED
    } else {
      SecurityManager.Result(true, q.getOrDefault("g", "default"))
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private val client: OkHttpClient by lazy { OkHttpClient.Builder().build() }
    private val PATTERN_SIGN = Pattern.compile("^[a-f0-9]+,(\\d+)$")
    private val PATTERN_NAMESPACE = Pattern.compile("[a-f0-9]{24}")
    private val FAILED = SecurityManager.Result(false, StringUtils.EMPTY)
  }


}