package `as`.leap.raptor.core

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.core.impl.DefaultAdaptor
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory
import org.testng.annotations.Test
import java.lang.invoke.MethodHandles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool

class AdaptorTest {

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

  @Test
  fun test() {
    val cdl = CountDownLatch(1)
    val vertx = Vertx.vertx()
    val nc = vertx.createNetClient()
    val addr = Address.from("rtmp://pili-publish.maxwon.cn/maxwon-live/qux?e=1491469885&token=Thphesb5UQHYEMKQspI4LrUUKO3gWd47rEvGdHcK:j3cKLk84CYPx3koCQru6jlLoRO4=")!!
    val adaptor = DefaultAdaptor(nc, addr, reconnect = 3)

    adaptor.onConnect {
      logger.info("connect!!!!")
      ForkJoinPool.commonPool().submit {
        Thread.sleep(3000L)
        logger.info("test close begin!")
        adaptor.backend?.close()
      }
    }

    adaptor.onClose {
      cdl.countDown()
      println("connect dead.")
    }
    adaptor.connect()
    cdl.await()
  }

}