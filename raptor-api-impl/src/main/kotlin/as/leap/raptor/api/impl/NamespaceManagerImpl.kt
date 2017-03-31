package `as`.leap.raptor.api.impl

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import com.google.common.base.Joiner
import com.google.common.base.Preconditions
import com.google.common.base.Splitter
import com.google.common.hash.Hashing
import org.apache.commons.lang3.StringUtils
import redis.clients.jedis.JedisCluster
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class NamespaceManagerImpl(private val jedis: JedisCluster) : NamespaceManager {

  override fun list(namespace: String): Map<String, Array<Address>> {
    check(namespace)
    return this.jedis.smembers(makeKey(namespace))
        .map { Pair(it, makeKey(namespace, it)) }
        .filter { this.jedis.exists(it.second) }
        .map {
          val addresses = this.jedis.smembers(it.second).map { decode(it) }.toTypedArray()
          Pair(it.first, addresses)
        }
        .toMap()
  }

  override fun clear(namespace: String, group: String) {
    check(namespace, group)
    this.jedis.srem(makeKey(namespace), group)
    this.jedis.del(makeKey(namespace, group))
  }

  override fun save(namespace: String, group: String, addresses: Array<Address>, expiresInSeconds: Int) {
    check(namespace, group)
    Preconditions.checkArgument(addresses.isNotEmpty(), "address array is empty!")
    Preconditions.checkArgument(expiresInSeconds in 1..3600, "expires must be between 0~3600!")
    var key = makeKey(namespace, group)
    this.jedis.sadd(key, *addresses.map { encode(it) }.toTypedArray())
    this.jedis.expire(key, expiresInSeconds)
    key = makeKey(namespace)
    if (expiresInSeconds > this.jedis.ttl(key)) {
      this.jedis.expire(key, expiresInSeconds)
    }
    this.jedis.sadd(key, group)
  }

  override fun exists(namespace: String, group: String): Boolean {
    check(namespace, group)
    return this.jedis.exists(makeKey(namespace, group))
  }

  override fun load(namespace: String, group: String): Array<Address> {
    check(namespace, group)
    val key = makeKey(namespace, group)
    if (!this.jedis.exists(key)) {
      return emptyArray()
    }
    return this.jedis.smembers(key).map { decode(it) }.toTypedArray()
  }

  override fun ttl(namespace: String, group: String, timeUnit: TimeUnit): Long {
    check(namespace, group)
    val k = makeKey(namespace, group)
    val exp = this.jedis.ttl(k)
    return timeUnit.convert(exp, TimeUnit.SECONDS)
  }

  companion object {

    private val PATTERN_NORMAL = Pattern.compile("[a-zA-Z0-9_]+")
    private val SPLITTER = "\u0001"

    private fun encode(address: Address): String {
      return when (address.port) {
        Address.DEFAULT_PORT -> Joiner.on(SPLITTER).join(address.host, address.context, address.key)
        else -> Joiner.on(SPLITTER).join(address.host, address.context, address.key, address.port)
      }
    }

    private fun decode(str: String): Address {
      val arr = Splitter.on(SPLITTER).splitToList(str)
      return when (arr.size) {
        3 -> Address(arr[0], arr[1], arr[2])
        4 -> Address(arr[0], arr[1], arr[2], arr[3].toInt())
        else -> throw IllegalArgumentException("invalid address serialization string: $str")
      }
    }

    private fun makeKey(namespace: String, group: String): String {
      val hash = Hashing.murmur3_128().newHasher()
          .putString(namespace, Charsets.UTF_8)
          .putString(StringUtils.LF, Charsets.UTF_8)
          .putString(group, Charsets.UTF_8)
          .hash()
          .toString()
      return "raptor$SPLITTER$hash"
    }

    private fun makeKey(namespace: String): String {
      val hash = Hashing.murmur3_128().newHasher()
          .putString(namespace, Charsets.UTF_8)
          .hash()
          .toString()
      return "raptor$SPLITTER$hash"
    }

    private fun check(namespace: String, group: String) {
      this.check(namespace)
      Preconditions.checkArgument(PATTERN_NORMAL.matcher(group).matches(), "illegal group $group.")
    }

    private fun check(namespace: String) {
      Preconditions.checkArgument(PATTERN_NORMAL.matcher(namespace).matches(), "illegal namespace $namespace.")
    }


  }

}