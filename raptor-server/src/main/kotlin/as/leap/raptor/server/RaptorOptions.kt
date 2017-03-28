package `as`.leap.raptor.server

data class RaptorOptions(
    val redis: String,
    val maxleap: String,
    val httpPort: Int = 8080,
    val rtmpPort: Int = 1935
)