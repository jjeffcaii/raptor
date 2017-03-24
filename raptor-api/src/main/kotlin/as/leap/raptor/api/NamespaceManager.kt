package `as`.leap.raptor.api

interface NamespaceManager {

  fun exists(namespace: String): Boolean

  fun address(namespace: String, streamKey: String): Array<Address>

}