package `as`.leap.raptor.server

import `as`.leap.raptor.commons.Utils
import `as`.leap.raptor.core.Swapper

data class RaptorOptions(
    var redis: String,
    var maxleap: String,
    var hostname: String = Utils.ipv4().first(),
    var httpPort: Int = 8080,
    var rtmpPort: Int = 1935,
    var reconnect: Int = 3,
    var strategy: Swapper.LiveStrategy = Swapper.LiveStrategy.ALL
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