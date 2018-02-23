package `in`.firedog.raptor.server.vo

data class PostGroup(
    val reconnect: Boolean = false,
    val expires: Int = 60,
    val addresses: List<PostAddress>
)