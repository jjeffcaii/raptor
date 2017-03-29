package `as`.leap.raptor.api.impl

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.commons.Utils
import com.google.common.base.Preconditions
import com.google.common.hash.Hashing
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisCluster
import java.lang.invoke.MethodHandles
import java.util.regex.Pattern

class NamespaceManagerImpl(private val jedis: JedisCluster) : NamespaceManager {

  override fun clear(namespace: String, group: String) {
    this.jedis.del(makeKey(namespace, group))
  }

  override fun save(namespace: String, group: String, addresses: Array<Address>, expiresInSeconds: Int) {
    Preconditions.checkArgument(PATTERN_NORMAL.matcher(namespace).matches(), "illegal namespace $namespace.")
    Preconditions.checkArgument(PATTERN_NORMAL.matcher(group).matches(), "illegal group $group.")
    Preconditions.checkArgument(addresses.isNotEmpty(), "load array is empty!")
    Preconditions.checkArgument(expiresInSeconds > 0, "expires must be greater than zero!")
    val key = makeKey(namespace, group)
    this.jedis.setex(key, Math.min(expiresInSeconds, 3600), Utils.toJSON(addresses))
  }

  override fun exists(namespace: String, group: String): Boolean {
    return this.jedis.exists(makeKey(namespace, group))
  }

  override fun load(namespace: String, group: String): Array<Address> {
    val key = makeKey(namespace, group)
    val str = this.jedis.get(key)
    if (str == null) {
      return emptyArray()
    } else {
      return Utils.fromJSONArray(str, Address::class.java)
    }
  }

  companion object {

    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private val PATTERN_NORMAL = Pattern.compile("[a-zA-Z0-9]+")

    private fun makeKey(namespace: String, group: String): String {
      val hash = Hashing.murmur3_128().newHasher()
          .putString(namespace, Charsets.UTF_8)
          .putString(StringUtils.LF, Charsets.UTF_8)
          .putString(group, Charsets.UTF_8)
          .hash()
          .toString()
      return "RAPTOR:$hash"
    }
  }

}