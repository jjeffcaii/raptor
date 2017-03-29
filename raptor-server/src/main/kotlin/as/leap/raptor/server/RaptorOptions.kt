package `as`.leap.raptor.server

import `as`.leap.raptor.commons.Utils

data class RaptorOptions(
    var redis: String,
    var maxleap: String,
    var httpPort: Int = 8080,
    var rtmpPort: Int = 1935
) {

  companion object {
    val production: RaptorOptions by lazy {
      Thread.currentThread().contextClassLoader.getResourceAsStream("raptor.production.json").use {
        Utils.fromJSON(it, RaptorOptions::class.java)
      }
    }
    val development: RaptorOptions by lazy {
      Thread.currentThread().contextClassLoader.getResourceAsStream("raptor.development.json").use {
        Utils.fromJSON(it, RaptorOptions::class.java)
      }
    }
  }
}