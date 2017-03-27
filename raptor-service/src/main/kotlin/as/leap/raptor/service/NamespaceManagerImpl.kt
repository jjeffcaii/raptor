package `as`.leap.raptor.service

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.commons.Utils
import com.google.common.base.Preconditions
import com.google.common.hash.Hashing
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisCluster
import java.lang.invoke.MethodHandles
import java.util.*

class NamespaceManagerImpl(private val jedis: JedisCluster) : NamespaceManager {

  override fun clear(namespace: String, group: String) {
    this.jedis.del(makeKey(namespace, group))
  }

  override fun set(namespace: String, group: String, addresses: Array<Address>, expiresInSeconds: Int) {
    Preconditions.checkArgument(addresses.isNotEmpty(), "address array is empty!")
    Preconditions.checkArgument(expiresInSeconds > 0, "expires must be greater than zero!")
    val key = makeKey(namespace, group)
    this.jedis.setex(key, Math.min(expiresInSeconds, 3600), Utils.toJSON(addresses))
  }

  override fun exists(namespace: String): Boolean {
    //TODO
    return true
  }

  override fun address(namespace: String, group: String): Array<Address> {
    val key = makeKey(namespace, group)
    val json = this.jedis.get(key)
    return when {
      Objects.isNull(json) || json.isBlank() -> emptyArray<Address>()
      else -> Utils.fromJSONArray(json, Address::class.java)
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private val prefix = "raptor"
    private fun makeKey(namespace: String, group: String): String {
      val hash = Hashing.murmur3_128().newHasher()
          .putString(namespace, Charsets.UTF_8)
          .putString(StringUtils.LF, Charsets.UTF_8)
          .putString(group, Charsets.UTF_8)
          .hash()
          .toString()
      return "$prefix:$hash"
    }
  }

}