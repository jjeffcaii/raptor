package `as`.leap.raptor.server

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.commons.Utils
import com.google.common.base.Throwables
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class RaptorVerticle(private val vertx: Vertx, private val namespaces: NamespaceManager) {

  init {
    val server = vertx.createHttpServer()
    val router = Router.router(this.vertx)

    router.post("/group/:group").handler { ctx ->
      val ob = Single.create<Array<Address>> {
        it.onSuccess(this.namespaces.address(ctx.get(KEY_NS), ctx.request().getParam("group")))
      }
      toJSON(ctx, ob)
    }

    server
        .requestHandler({ router.accept(it) })
        .listen(8080, {
          when (it.succeeded()) {
            true -> logger.info("raptor server start success!")
            else -> logger.error("raptor server start failed.", it.cause())
          }
        })
  }


  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private val KEY_NS = "K_ID"
    private val KEY_SECRET = "K_SC"

    private fun toJSON(ctx: RoutingContext, ob: Single<*>, statusCode: Int = 200) {
      ob.subscribeOn(Schedulers.io()).subscribe({
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("Content-Type", "application/json; charset=utf-8")
            .end(Utils.toJSON(it))
      }, {
        ctx.response().setStatusCode(500).end(Throwables.getStackTraceAsString(it.cause))
      })
    }

  }


}