package `as`.leap.raptor.server

import org.testng.Assert
import org.testng.annotations.Test

class RaptorOptionsTest {

  @Test
  fun test() {
    val production = RaptorOptions.production
    val development = RaptorOptions.development
    Assert.assertNotNull(development)
    Assert.assertNotNull(production)
  }

}

