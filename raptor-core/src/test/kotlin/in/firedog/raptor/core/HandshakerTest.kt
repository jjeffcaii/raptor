package `in`.firedog.raptor.core

import `in`.firedog.raptor.core.impl.endpoint.LazyEndpoint
import `in`.firedog.raptor.core.impl.ext.Handshaker
import io.vertx.core.Vertx
import org.testng.Assert
import org.testng.annotations.Test
import java.util.concurrent.CountDownLatch

class HandshakerTest {

  private val vertx = Vertx.vertx()
  private val netClient = vertx.createNetClient()

  @Test
  fun testInitiative() {
    arrayOf("pili-publish.maxwon.cn", "send3.douyu.com", "dl.live-send.acg.tv").forEach {
      Assert.assertTrue(this.testInitiative(it))
    }
  }

  private fun testInitiative(host: String): Boolean {
    val endpoint = LazyEndpoint(netClient, host)
    val cdl = CountDownLatch(1)
    var connected = false
    val handshaker = Handshaker(endpoint, {
      connected = true
      cdl.countDown()
    }, {
      cdl.countDown()
    }, printDetails = true)
    endpoint.onHandshake { handshaker.validate(it) }
    cdl.await()
    endpoint.close()
    return connected
  }


}