package `as`.leap.raptor.server

object BootstrapTest {

  @JvmStatic
  fun main(args: Array<String>) {
    System.setProperty("raptor.server.www", "raptor-server/src/main/www/dist")
    Bootstrap.main(emptyArray())
  }

}