package `as`.leap.raptor.api

import org.testng.Assert
import org.testng.annotations.Test

class AddressTest {

  @Test
  fun test() {
    val address = Address.from("rtmp://abc.com/foobar?st=a")
    Assert.assertNotNull(address)
    Assert.assertEquals("abc.com", address?.host)
    Assert.assertEquals(1935, address?.port)
    Assert.assertEquals("foobar", address?.context)
    Assert.assertEquals("?st=a", address?.key)
    println(address)
  }

  @Test
  fun test2(){
    val address = Address.from("rtmp://abc.com:1933/foobar/st=a")
    Assert.assertNotNull(address)
    Assert.assertEquals("abc.com", address?.host)
    Assert.assertEquals(1933, address?.port)
    Assert.assertEquals("foobar", address?.context)
    Assert.assertEquals("/st=a", address?.key)
    println(address)
  }


}