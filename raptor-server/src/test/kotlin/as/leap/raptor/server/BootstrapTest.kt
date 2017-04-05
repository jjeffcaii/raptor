package `as`.leap.raptor.server

object BootstrapTest {

  @JvmStatic
  fun main(args: Array<String>) {
    Bootstrap.main(arrayOf("--www", "raptor-server/src/main/www/dist"))
  }

}