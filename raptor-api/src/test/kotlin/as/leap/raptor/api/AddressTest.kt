package `as`.leap.raptor.api

import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotNull
import org.testng.annotations.Test

class AddressTest {

  @Test
  fun test() {
    val address = Address.from("rtmp://abc.com/foobar?st=a")
    assertNotNull(address)
    assertEquals("abc.com", address?.host)
    assertEquals(1935, address?.port)
    assertEquals("foobar", address?.context)
    assertEquals("?st=a", address?.key)
    println(address)
  }

  @Test
  fun test2() {
    val address = Address.from("rtmp://abc.com:1933/foobar/st=a")
    assertNotNull(address)
    assertEquals("abc.com", address?.host)
    assertEquals(1933, address?.port)
    assertEquals("foobar", address?.context)
    assertEquals("/st=a", address?.key)
    println(address)
  }

  @Test
  fun test3() {
    val app = "foobar/st=a"
    val ret = Address.extractFull(app)
    assertNotNull(ret)
    assertEquals("foobar", ret!!.first)
    assertEquals("/st=a", ret.second)
  }

}