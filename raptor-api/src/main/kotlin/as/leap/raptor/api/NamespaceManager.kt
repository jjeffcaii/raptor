package `as`.leap.raptor.api

interface NamespaceManager {
  fun exists(namespace: String): Boolean
  fun clear(namespace: String, group: String)
  fun set(namespace: String, group: String, addresses: Array<Address>, expiresInSeconds: Int = 60)
  fun address(namespace: String, group: String): Array<Address>
}