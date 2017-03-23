package `as`.leap.raptor.service

import org.testng.Assert
import org.testng.annotations.Test

class SimpleNamespaceManagerTest {

  private val manager = SimpleNamespaceManager()

  @Test
  fun test() {
    val ns = "maxwon-live"
    Assert.assertTrue(manager.exists(ns))
    Assert.assertFalse(manager.exists("xxxxyyyy"))

    val arr = manager.address(ns, "/foo,bar,foobar")
    Assert.assertEquals(3, arr.size)

    arr.forEach {
      println(it.toString())
    }


  }

}

