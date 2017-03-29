package `as`.leap.raptor.service

import `as`.leap.raptor.api.SecurityManager
import `as`.leap.raptor.commons.Consts
import com.google.common.base.Splitter
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.regex.Pattern

class SecurityManagerImpl(endpoint: String) : SecurityManager {

  private val base: String = "${StringUtils.strip(endpoint, "/")}/2.0/acl"
  private val cache: LoadingCache<Pair<String, String>, Boolean> = CacheBuilder.newBuilder()
      .maximumSize(1024)
      .concurrencyLevel(4)
      .build(CacheLoader.from { p ->
        val req = Request.Builder().url(this.base)
            .header(Consts.HEADER_MAXLEAP_APPID, p!!.first)
            .header(Consts.HEADER_MAXLEAP_SIGN, p.second)
            .header(Consts.HEADER_CONTENT_TYPE, Consts.CONTENT_TYPE_JSON_UTF8)
            .get()
            .build()
        var success: Boolean = false
        client.newCall(req).execute().use {
          if (logger.isDebugEnabled) {
            logger.debug("maxleap acl response({}): {}.", it.code(), it.body().string())
          }
          success = it.isSuccessful
        }
        success
      })

  override fun exists(namespace: String): Boolean {
    //TODO validate appid exists.
    return PATTERN_NAMESPACE.matcher(namespace).matches()
  }

  override fun validate(namespace: String, streamKey: String): SecurityManager.Result {
    val sk = if (streamKey.startsWith("/") || streamKey.startsWith("?")) {
      streamKey.substring(1)
    } else {
      streamKey
    }
    val q: Map<String, String> = Splitter.on("&").withKeyValueSeparator("=").split(sk)
    val sign = q[Consts.KEY_FOR_SIGN]

    if (sign.isNullOrBlank()) {
      if (logger.isDebugEnabled) {
        logger.debug("security validate failed: token is blank.")
      }
      return FAILED
    }

    if (StringUtils.equals(sign, SecurityManager.GOD_KEY)) {
      return SecurityManager.Result(true, q.getOrDefault(Consts.KEY_FOR_GROUP, Consts.DEFAULT_GROUP_NAME))
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

    return if (this.cache.get(Pair(namespace, sign!!))) {
      SecurityManager.Result(true, q.getOrDefault(Consts.KEY_FOR_GROUP, Consts.DEFAULT_GROUP_NAME))
    } else {
      FAILED
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