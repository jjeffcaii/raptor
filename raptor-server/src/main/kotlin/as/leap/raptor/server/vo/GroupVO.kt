package `as`.leap.raptor.server.vo

data class GroupVO(val name: String, val addresses: List<PostAddress>, val expires: Long? = null)
