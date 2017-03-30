package `as`.leap.raptor.api

import java.util.concurrent.TimeUnit

interface NamespaceManager {
  fun exists(namespace: String, group: String): Boolean
  fun clear(namespace: String, group: String)
  fun save(namespace: String, group: String, addresses: Array<Address>, expiresInSeconds: Int = 60)
  fun load(namespace: String, group: String): Array<Address>
  fun ttl(namespace: String, group: String, timeUnit: TimeUnit = TimeUnit.SECONDS): Long
  fun list(namespace: String): Map<String, Array<Address>>
}