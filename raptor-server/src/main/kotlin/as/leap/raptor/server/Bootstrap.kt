package `as`.leap.raptor.server

object Bootstrap {

  @JvmStatic
  fun main(args: Array<String>) {
    val env = System.getenv("RAPTOR_ENV")
    val opts: RaptorOptions = when (env) {
      "production" -> RaptorOptions.production
      else -> RaptorOptions.development
    }
    val server = RaptorServer(opts)
    server.run()
  }

}