package `as`.leap.raptor.api

interface NamespaceManager {

  fun exists(app: String): Boolean

  companion object {
    val INSTANCE: NamespaceManager = object : NamespaceManager {
      override fun exists(app: String): Boolean {
        return false
      }
    }

  }

}