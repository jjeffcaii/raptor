package `as`.leap.raptor.server

import io.vertx.core.Vertx
import io.vertx.kotlin.core.http.RequestOptions
import org.testng.Assert
import org.testng.annotations.Test
import java.util.concurrent.CountDownLatch

class RaptorOptionsTest {

  @Test
  fun test() {
    val production = RaptorOptions.production
    val development = RaptorOptions.development
    Assert.assertNotNull(development)
    Assert.assertNotNull(production)
  }

  @Test
  fun test2() {
    val cdl = CountDownLatch(1)
    val vertx = Vertx.vertx()
    val c = vertx.createHttpClient()
    val requestOptions = RequestOptions("http://baidu.com/")
    c.getNow(requestOptions, {
      println(it.statusCode())
      cdl.countDown()
    })
    cdl.await()




  }

}

