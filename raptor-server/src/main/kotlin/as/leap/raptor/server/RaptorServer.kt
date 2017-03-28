package `as`.leap.raptor.server

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.api.SecurityManager
import `as`.leap.raptor.commons.Utils
import `as`.leap.raptor.commons.exception.RaptorException
import `as`.leap.raptor.core.impl.DefaultSwapper
import `as`.leap.raptor.service.NamespaceManagerImpl
import `as`.leap.raptor.service.SecurityManagerImpl
import com.google.common.base.Splitter
import com.google.common.net.HostAndPort
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.net.NetServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisCluster
import java.lang.invoke.MethodHandles

class RaptorServer(private val opts: RaptorOptions) : Runnable {

  private val vertx = Vertx.vertx()
  private val apiServer: HttpServer
  private val rtmpServer: NetServer
  private val namespaceManager: NamespaceManager
  private val securityManager: SecurityManager

  init {
    // 0. preparement.
    val seeds = Splitter.on(",").splitToList(opts.redis)
        .map {
          val hap = HostAndPort.fromString(it)
          redis.clients.jedis.HostAndPort(hap.hostText, hap.getPortOrDefault(6399))
        }
        .toSet()
    val jedis = JedisCluster(seeds)
    Runtime.getRuntime().addShutdownHook(Thread({ jedis.close() }))
    this.namespaceManager = NamespaceManagerImpl(jedis)
    this.securityManager = SecurityManagerImpl(opts.maxleap)

    // 1. create restful server.
    this.apiServer = vertx.createHttpServer()
    val router = Router.router(this.vertx)
    router.route().handler(BodyHandler.create())
    router.route().handler {
      it.response()
          .putHeader("Access-Control-Allow-Origin", "*")
          .putHeader("Access-Control-Allow-Methods", "HEAD, POST, GET, OPTIONS, DELETE, PUT")
          .putHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept")
      when (it.request().method()) {
        HttpMethod.OPTIONS -> it.response().end()
        else -> it.next()
      }
    }

    router.post("/:ns/groups/:gp").consumes(CONTENT_TYPE_JSON).handler { ctx ->
      val ob = Single.create<Int> {
        val ns = ctx.request().getParam("ns")
        val gp = ctx.request().getParam("gp")
        if (this.namespaceManager.exists(ns, gp)) {
          throw RaptorException("group $gp exists already!")
        }
        val addresses = Utils.fromJSONArray(ctx.bodyAsString, Address::class.java)
        logger.info("got addresses: {}", addresses)
        this.namespaceManager.save(ns, gp, addresses)
        it.onSuccess(addresses.size)
      }
      toJSON(ctx, ob, 201)
    }

    router.put("/:ns/groups/:gp").consumes(CONTENT_TYPE_JSON).handler { ctx ->
      val ob = Single.create<Int> {
        val ns = ctx.request().getParam("ns")
        val gp = ctx.request().getParam("gp")
        if (!this.namespaceManager.exists(ns, gp)) {
          throw RaptorException("group $gp doesn't exists!")
        }
        val addresses = Utils.fromJSONArray(ctx.bodyAsString, Address::class.java)
        this.namespaceManager.save(ns, gp, addresses)
        it.onSuccess(addresses.size)
      }
      toJSON(ctx, ob, 201)
    }

    router.delete("/:ns/groups/:gp").handler { ctx ->
      val ob = Single.create<Int> {
        val ns = ctx.request().getParam("ns")
        val gp = ctx.request().getParam("gp")
        this.namespaceManager.clear(ns, gp)
        it.onSuccess(1)
      }
      toJSON(ctx, ob, 204)
    }

    router.get("/:ns/groups/:gp").handler { ctx ->
      val ob = Single.create<Array<Address>> {
        val ns = ctx.request().getParam("ns")
        val gp = ctx.request().getParam("gp")
        it.onSuccess(this.namespaceManager.load(ns, gp))
      }
      toJSON(ctx, ob)
    }


    this.apiServer.requestHandler({ router.accept(it) })
    this.rtmpServer = vertx.createNetServer()

    // 2. create rtmp server.
    val nc = vertx.createNetClient()
    this.rtmpServer.connectHandler {
      it.pause()
      DefaultSwapper(it, nc, namespaceManager, securityManager)
      it.resume()
    }
  }

  override fun run() {
    this.apiServer.listen(this.opts.httpPort, {
      when (it.succeeded()) {
        true -> logger.info("API server start success!")
        else -> {
          logger.error("API server start failed!!!", it.cause())
          System.exit(1)
        }
      }
    })

    this.rtmpServer.listen(this.opts.rtmpPort, {
      when (it.succeeded()) {
        true -> logger.info("RTMP server start success!")
        else -> {
          logger.error("RTMP server start failed!!!", it.cause())
          System.exit(2)
        }
      }
    })
  }

  companion object {

    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private val HEADER_CONTENT_TYPE = "Content-Type"
    private val CONTENT_TYPE_JSON = "application/json"
    private val CONTENT_TYPE_JSON_UTF8 = "application/json; charset=utf-8"


    private fun toJSON(ctx: RoutingContext, ob: Single<*>, statusCode: Int = 200) {
      ob.subscribeOn(Schedulers.io()).subscribe({
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON_UTF8)
            .end(Utils.toJSON(it))
      }, {
        val resp = ctx.response()
            .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON_UTF8)
        var code: Int = 500
        var ecode: Int = 5000
        var msg: String = StringUtils.EMPTY

        it.cause?.let { ex ->
          ex.message?.let { msg = it }
          when (ex) {
            is RaptorException -> {
              val c = ex.toCode()
              code = c.second
              ecode = c.first
            }
            is IllegalArgumentException -> {
              code = 400
            }
            else -> {
              //ignore
            }
          }
        }
        resp.setStatusCode(code).end(Utils.toJSON(mapOf("code" to ecode, "msg" to msg)))
      })
    }

  }

}