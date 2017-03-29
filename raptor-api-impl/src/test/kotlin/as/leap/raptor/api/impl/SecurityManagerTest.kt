package `as`.leap.raptor.api.impl

import `as`.leap.raptor.api.SecurityManager
import `as`.leap.raptor.api.impl.SecurityManagerImpl
import org.apache.commons.codec.digest.DigestUtils
import org.testng.Assert
import org.testng.annotations.Test

class SecurityManagerTest {

  private val manager: SecurityManager = SecurityManagerImpl("https://apiuat.maxleap.cn/")

  private val namespace: String = "5795ad33aa150a0001fcbfa3"

  @Test
  fun testNormal() {
    val k = "QXNJZnpNRDBZejhfbmpwRjlBVk5Bdw"
    val ts = System.currentTimeMillis() + 60000L
    val hash = DigestUtils.md5Hex("$ts$k")
    Assert.assertTrue(manager.validate(this.namespace, "?k=$hash,$ts").success)
    Assert.assertTrue(manager.validate(this.namespace, "k=$hash,$ts").success)
    Assert.assertTrue(manager.validate(this.namespace, "/k=$hash,$ts").success)
  }

  @Test
  fun testGod() {
    Assert.assertTrue(manager.validate(this.namespace, "?k=${SecurityManager.GOD_KEY}").success)
  }

  @Test
  fun testBad() {
    Assert.assertFalse(manager.validate(this.namespace, "?k=1234,${Long.MAX_VALUE}").success)
    Assert.assertFalse(manager.validate(this.namespace, "?k=").success)
    val k = "QXNJZnpNRDBZejhfbmpwRjlBVk5Bdw"
    val ts = System.currentTimeMillis() - 3000L
    val hash = DigestUtils.md5Hex("$ts$k")
    Assert.assertFalse(manager.validate(this.namespace, "?k=$hash,$ts").success)
  }


}