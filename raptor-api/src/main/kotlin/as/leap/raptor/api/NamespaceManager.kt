package `as`.leap.raptor.api

interface NamespaceManager {
  fun exists(namespace: String, group: String): Boolean
  fun clear(namespace: String, group: String)
  fun save(namespace: String, group: String, addresses: Array<Address>, expiresInSeconds: Int = 60)
  fun load(namespace: String, group: String): Array<Address>
}