package `as`.leap.raptor.core

import `as`.leap.raptor.core.impl.endpoint.LazyEndpoint
import `as`.leap.raptor.core.impl.ext.Handshaker
import `as`.leap.raptor.core.utils.CodecHelper
import org.testng.Assert
import org.testng.annotations.Test
import java.util.concurrent.CountDownLatch

class HandshakerTest {

  @Test
  fun testInitiative() {
    arrayOf("pili-publish.maxwon.cn", "send3.douyu.com", "dl.live-send.acg.tv").forEach {
      Assert.assertTrue(this.testInitiative(it))
    }
  }

  private fun testInitiative(host: String): Boolean {
    val endpoint = LazyEndpoint(host)
    val cdl = CountDownLatch(1)
    var connected: Boolean = false
    val handshaker = Handshaker(endpoint, {
      connected = true
      cdl.countDown()
    }, {
      cdl.countDown()
    })
    endpoint.onHandshake {
      val buffer = it.toBuffer()
      if (buffer.length() < 64) {
        println(CodecHelper.encodeHex(buffer.bytes, true))
      } else {
        println(CodecHelper.encodeHex(buffer.slice(0, 64).bytes, true))
      }
      println("$host--------------------------------------")
      handshaker.validate(it)
    }
    cdl.await()
    endpoint.close()
    return connected
  }


}