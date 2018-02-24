package me.zarafa.raptor.server

import me.zarafa.raptor.commons.Utils
import me.zarafa.raptor.core.Swapper

data class RaptorOptions(
    var hostname: String = Utils.ipv4().first(),
    var httpPort: Int = 8080,
    var rtmpPort: Int = 1935,
    var reconnect: Int = 3,
    var www: String? = null,
    var strategy: Swapper.LiveStrategy = Swapper.LiveStrategy.ALL
)