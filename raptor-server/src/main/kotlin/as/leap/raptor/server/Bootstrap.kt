package `as`.leap.raptor.server

import org.apache.commons.cli.*
import java.util.*


object Bootstrap {

  private val version: String? by lazy {
    Thread.currentThread().contextClassLoader.getResourceAsStream("raptor.version.properties").use {
      val p = Properties()
      p.load(it)
      p["version"] as? String
    }
  }

  @JvmStatic
  fun main(args: Array<String>) {
    val options = Options()
        .addOption(null, "http-port", true, "http port. (default is 8080)")
        .addOption(null, "rtmp-port", true, "rtmp port. (default is 1935)")
        .addOption(null, "production", false, "use production mode.")
        .addOption(null, "hostname", true, "rtmp hostname. (default is host_ip)")
        .addOption(null, "www", true, "static pages dir.")
        .addOption(null, "help", false, "print usage.")
        .addOption(null, "version", false, "print version.")

    val parser = DefaultParser()
    val result: CommandLine
    try {
      result = parser.parse(options, args)
    } catch (e: ParseException) {
      val formatter = HelpFormatter()
      formatter.printHelp("raptor", options)
      throw e
    }

    if (result.hasOption("help")) {
      val formatter = HelpFormatter()
      formatter.printHelp("raptor", options)
      return
    }

    if (result.hasOption("version")) {
      println("raptor $version")
      return
    }

    val logo = """
                     __
   _________ _____  / /_____  _____
  / ___/ __ `/ __ \/ __/ __ \/ ___/
 / /  / /_/ / /_/ / /_/ /_/ / /
/_/   \__,_/ .___/\__/\____/_/
          /_/               v.$version"""

    println(logo)
    println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")

    val opts = if (result.hasOption("production")) {
      RaptorOptions.production
    } else {
      RaptorOptions.development
    }
    if (result.hasOption("http-port")) {
      opts.httpPort = result.getOptionValue("http-port").toInt()
    }
    if (result.hasOption("rtmp-port")) {
      opts.rtmpPort = result.getOptionValue("rtmp-port").toInt()
    }
    if (result.hasOption("hostname")) {
      opts.hostname = result.getOptionValue("hostname")
    }
    val server = if (result.hasOption("www")) {
      RaptorServer(opts, result.getOptionValue("www"))
    } else {
      RaptorServer(opts)
    }
    server.run()
  }

}