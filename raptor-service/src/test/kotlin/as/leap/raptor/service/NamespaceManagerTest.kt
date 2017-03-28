package `as`.leap.raptor.service

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.commons.Utils
import org.testng.Assert
import org.testng.annotations.Test

class NamespaceManagerTest {

  @Test
  fun test() {
    val address = Address.from("rtmp://abc.com/ctx?k=1")
    val json = Utils.toJSON(address!!)
    val address2 = Utils.fromJSON(json, Address::class.java)
    Assert.assertEquals(address, address2)
  }


}