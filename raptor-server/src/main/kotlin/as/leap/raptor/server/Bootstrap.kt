package `as`.leap.raptor.server

object Bootstrap {

  @JvmStatic
  fun main(args: Array<String>) {
    val opts = RaptorOptions("1.redis.cluster:6399,2.redis.cluster:6399,3.redis.cluster:6399", "http://apiuat.maxleap.cn/")
    val server = RaptorServer(opts)
    server.run()
  }

}