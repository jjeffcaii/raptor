package `as`.leap.raptor.server

object Bootstrap {

  @JvmStatic
  fun main(args: Array<String>) {
    val logo = """
                     __
   _________ _____  / /_____  _____
  / ___/ __ `/ __ \/ __/ __ \/ ___/
 / /  / /_/ / /_/ / /_/ /_/ / /
/_/   \__,_/ .___/\__/\____/_/
          /_/"""

    println(logo)
    val env = System.getenv("RAPTOR_ENV")
    val opts: RaptorOptions = when (env) {
      "production" -> RaptorOptions.production
      else -> RaptorOptions.development
    }
    val server = RaptorServer(opts, System.getProperty("raptor.server.www"))
    server.run()
  }

}