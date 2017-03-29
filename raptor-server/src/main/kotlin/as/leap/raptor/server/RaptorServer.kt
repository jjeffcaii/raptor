package `as`.leap.raptor.server

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.api.SecurityManager
import `as`.leap.raptor.api.exception.RaptorException
import `as`.leap.raptor.api.impl.NamespaceManagerImpl
import `as`.leap.raptor.api.impl.SecurityManagerImpl
import `as`.leap.raptor.commons.Consts
import `as`.leap.raptor.commons.Utils
import `as`.leap.raptor.core.impl.DefaultSwapper
import `as`.leap.raptor.server.vo.PostGroup
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
import io.vertx.ext.web.handler.StaticHandler
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisCluster
import java.io.File
import java.lang.invoke.MethodHandles

class RaptorServer(private val opts: RaptorOptions) : Runnable {

  private val vertx = Vertx.vertx()
  private val apiServer: HttpServer
  private val rtmpServer: NetServer
  private val namespaceManager: NamespaceManager
  private val securityManager: SecurityManager

  init {
    // 0. preparement.
    val seeds = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(opts.redis)
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

    System.getenv("RAPTOR_HOME")?.let {
      val f = File(it, "www")
      if (f.exists() && f.isDirectory) {
        router.route("/*").handler(StaticHandler.create(f.absolutePath))
      }
    }

    router.route().handler(BodyHandler.create())
    router.route().handler {
      it.response()
          .putHeader(Consts.HEADER_CORS_ORIGIN, Consts.CORS_ORIGIN)
          .putHeader(Consts.HEADER_CORS_METHOD, Consts.CORS_METHOD)
          .putHeader(Consts.HEADER_CORS_HEADER, Consts.CORS_HEADER)
      when (it.request().method()) {
        HttpMethod.OPTIONS -> it.response().end()
        else -> it.next()
      }
    }

    router.post("/:ns/groups/:gp").consumes(Consts.CONTENT_TYPE_JSON).handler(this::saveGroup)
    router.put("/:ns/groups/:gp").consumes(Consts.CONTENT_TYPE_JSON).handler(this::saveGroup)

    router.delete("/:ns/groups/:gp").handler { ctx ->
      val ob = Single.create<Int> {
        val ns = ctx.request().getParam("ns")
        val gp = ctx.request().getParam("gp")
        this.namespaceManager.clear(ns, gp)
        it.onSuccess(1)
      }
      consumeAsJSON(ctx, ob, 204)
    }
    router.head("/:ns/groups/:gp").handler { ctx ->
      val ob = Single.create<Boolean> {
        val ns = ctx.request().getParam("ns")
        val gp = ctx.request().getParam("gp")
        it.onSuccess(this.namespaceManager.exists(ns, gp))
      }
      ob.subscribeOn(Schedulers.io()).subscribe({
        val statusCode = when (it) {
          true -> 200
          false -> 404
        }
        ctx.response().setStatusCode(statusCode).end()
      }, {
        ctx.response().setStatusCode(500).end()
      })
    }

    router.get("/:ns/groups/:gp").handler { ctx ->
      val ob = Single.create<Array<Address>> {
        val ns = ctx.request().getParam("ns")
        val gp = ctx.request().getParam("gp")
        it.onSuccess(this.namespaceManager.load(ns, gp))
      }
      consumeAsJSON(ctx, ob)
    }

    this.apiServer.requestHandler({ router.accept(it) })

    // 2. create rtmp server.
    this.rtmpServer = vertx.createNetServer()
    val netClient = vertx.createNetClient()
    this.rtmpServer.connectHandler {
      it.pause()
      DefaultSwapper(it, netClient, namespaceManager, securityManager)
      it.resume()
    }
  }

  private fun saveGroup(ctx: RoutingContext) {
    val ob = Single.create<Int> {
      val ns = ctx.request().getParam("ns")
      val gp = ctx.request().getParam("gp")
      val vo = Utils.fromJSON(ctx.bodyAsString, PostGroup::class.java)
      val addresses = vo.addresses.map { it.toAddress()!! }.toList().toTypedArray()
      this.namespaceManager.save(ns, gp, addresses, vo.expires)
      it.onSuccess(addresses.size)
    }
    consumeAsJSON(ctx, ob, 201)
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

    private fun consumeAsJSON(ctx: RoutingContext, ob: Single<*>, statusCode: Int = 200) {
      ob.subscribeOn(Schedulers.io()).subscribe({
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader(Consts.HEADER_CONTENT_TYPE, Consts.CONTENT_TYPE_JSON_UTF8)
            .end(Utils.toJSON(it))
      }, {
        val resp = ctx.response()
            .putHeader(Consts.HEADER_CONTENT_TYPE, Consts.CONTENT_TYPE_JSON_UTF8)
        var code: Int = 500
        var ecode: Int = 5000
        var msg: String? = null

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
          }
        }
        resp.setStatusCode(code).end(Utils.toJSON(mapOf("code" to ecode, "msg" to msg)))
      })
    }

  }

}