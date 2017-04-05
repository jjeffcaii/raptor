package `as`.leap.raptor.server

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.api.SecurityManager
import `as`.leap.raptor.api.exception.RaptorException
import `as`.leap.raptor.api.impl.NamespaceManagerOverRedis
import `as`.leap.raptor.api.impl.SecurityManagerImpl
import `as`.leap.raptor.commons.Consts
import `as`.leap.raptor.commons.Errors
import `as`.leap.raptor.commons.Utils
import `as`.leap.raptor.core.impl.DefaultSwapper
import `as`.leap.raptor.server.vo.GroupVO
import `as`.leap.raptor.server.vo.PostAddress
import `as`.leap.raptor.server.vo.PostGroup
import `as`.leap.raptor.server.vo.Shit
import com.google.common.base.Joiner
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
import io.vertx.kotlin.core.net.NetServerOptions
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisCluster
import java.io.File
import java.lang.invoke.MethodHandles
import java.util.concurrent.TimeUnit

class RaptorServer(private val opts: RaptorOptions, www: String? = null) : Runnable {

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
    this.namespaceManager = NamespaceManagerOverRedis(jedis)
    this.securityManager = SecurityManagerImpl(opts.maxleap)

    // 1. create restful server.
    this.apiServer = vertx.createHttpServer()
    val router = Router.router(this.vertx)

    www?.let {
      val f = File(it)
      if (f.exists() && f.isDirectory) {
        if (logger.isDebugEnabled) {
          logger.debug("page files found: {}.", f.absolutePath)
        }
        router.route("/console/*").handler(StaticHandler.create(it))
        router.get("/favicon.ico").handler { ctx ->
          ctx.response().sendFile(Joiner.on("/").join(arrayOf(it, "favicon.ico")))
        }
      }
    }

    router.route().handler(BodyHandler.create())

    // enable cross domain.
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

    // permission check.
    router.route().handler { ctx ->
      val ob = Single.create<Triple<Boolean, String, String>> {
        val ns = ctx.request().getHeader(Consts.HEADER_MAXLEAP_APPID)
        val tk = ctx.request().getHeader(Consts.HEADER_MAXLEAP_APIKEY)
        it.onSuccess(Triple(this.securityManager.nativeValidate(ns, tk), ns, tk))
      }
      ob.subscribeOn(Schedulers.io()).subscribe({
        when (it.first) {
          true -> ctx.put(KEY_NS, it.second).put(KEY_TK, it.third).next()
          else -> {
            ctx.response()
                .putHeader(Consts.HEADER_CONTENT_TYPE, Consts.CONTENT_TYPE_JSON_UTF8)
                .setStatusCode(401)
                .end(Shit(Errors.security, "Not valid APP_ID or API_KEY!").toString())
          }
        }
      }, { ctx.response().setStatusCode(500).end() })
    }

    router.post("/groups/:gp").consumes(Consts.CONTENT_TYPE_JSON).handler { ctx ->
      val ob = Single.create<Int> {
        val ns: String = ctx.get(KEY_NS)
        val gp = ctx.request().getParam("gp")
        val vo = Utils.fromJSON(ctx.bodyAsString, PostGroup::class.java)
        val addresses = vo.addresses.map { it.toAddress()!! }.toList().toTypedArray()
        if (this.namespaceManager.exists(ns, gp)) {
          throw RaptorException("group $gp exists already!").code(Errors.conflict)
        }
        this.namespaceManager.save(ns, gp, addresses, vo.expires)
        it.onSuccess(1)
      }
      consumeAsJSON(ctx, ob, 201)
    }

    router.put("/groups/:gp").consumes(Consts.CONTENT_TYPE_JSON).handler { ctx ->
      val ob = Single.create<Int> {
        val ns: String = ctx.get(KEY_NS)
        val gp = ctx.request().getParam("gp")
        val vo = Utils.fromJSON(ctx.bodyAsString, PostGroup::class.java)
        val addresses = vo.addresses.map { it.toAddress()!! }.toList().toTypedArray()
        this.namespaceManager.clear(ns, gp)
        this.namespaceManager.save(ns, gp, addresses, vo.expires)
        it.onSuccess(1)
      }
      consumeAsJSON(ctx, ob)
    }

    router.patch("/groups/:gp").consumes(Consts.CONTENT_TYPE_JSON).handler { ctx ->
      val ob = Single.create<Int> {
        val ns: String = ctx.get(KEY_NS)
        val gp = ctx.request().getParam("gp")
        val vo = Utils.fromJSON(ctx.bodyAsString, PostGroup::class.java)
        val addresses = vo.addresses.map { it.toAddress()!! }.toList().toTypedArray()
        this.namespaceManager.save(ns, gp, addresses, vo.expires)
        it.onSuccess(1)
      }
      consumeAsJSON(ctx, ob)
    }

    router.delete("/groups/:gp").handler { ctx ->
      val ob = Single.create<Int> {
        val ns: String = ctx.get(KEY_NS)
        val gp = ctx.request().getParam("gp")
        this.namespaceManager.clear(ns, gp)
        it.onSuccess(1)
      }
      consumeAsJSON(ctx, ob, 204)
    }
    router.head("/groups/:gp").handler { ctx ->
      val ob = Single.create<Boolean> {
        val ns: String = ctx.get(KEY_NS)
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

    router.get("/groups").handler { ctx ->
      val ob = Single.create<List<GroupVO>> {
        val ns: String = ctx.get(KEY_NS)
        val li = this.namespaceManager.list(ns)
            .map {
              val addresses = it.value.map { PostAddress(it.toBaseURL(), it.key) }.toList()
              val exp = this.namespaceManager.ttl(ns, it.key)
              GroupVO(it.key, addresses, exp)
            }
            .toList()
        it.onSuccess(li)
      }
      consumeAsJSON(ctx, ob)
    }

    router.get("/groups/:gp").handler { ctx ->
      val ob = Single.create<GroupVO> {
        val ns: String = ctx.get(KEY_NS)
        val gp = ctx.request().getParam("gp")
        when (this.namespaceManager.exists(ns, gp)) {
          true -> {
            val addresses = this.namespaceManager.load(ns, gp).map { PostAddress(it.toBaseURL(), it.key) }.toList()
            val exp = this.namespaceManager.ttl(ns, gp)
            it.onSuccess(GroupVO(gp, addresses, exp))
          }
          else -> throw RaptorException("no such group $gp.").code(Errors.missing, 404)
        }
      }
      consumeAsJSON(ctx, ob)
    }

    router.get("/groups/:gp/publish").handler { ctx ->
      val ob = Single.create<Any> {
        val ns: String = ctx.get(KEY_NS)
        val gp = ctx.request().getParam("gp")
        val tk: String = ctx.get(KEY_TK)
        when (this.namespaceManager.exists(ns, gp)) {
          true -> {
            val ts = System.currentTimeMillis() + this.namespaceManager.ttl(ns, gp, TimeUnit.MILLISECONDS)
            val sign: String
            when (tk) {
              SecurityManager.GOD_KEY -> sign = tk
              else -> {
                val hash = DigestUtils.md5Hex("$ts$tk")
                sign = "$hash,$ts"
              }
            }
            val url = when (opts.rtmpPort) {
              Address.DEFAULT_PORT -> "rtmp://${opts.hostname}/$ns?k=$sign&g=$gp"
              else -> "rtmp://${opts.hostname}:${opts.rtmpPort}/$ns?k=$sign&g=$gp"
            }
            it.onSuccess(mapOf("url" to url, "ts" to ts))
          }
          else -> throw RaptorException("no such group $gp.").code(Errors.missing, 404)
        }
      }
      consumeAsJSON(ctx, ob)
    }

    router.get("/ok").handler { ctx ->
      ctx.response().putHeader(Consts.HEADER_CONTENT_TYPE, Consts.CONTENT_TYPE_JSON_UTF8).end("1")
    }

    this.apiServer.requestHandler({ router.accept(it) })

    // 2. create rtmp server.
    this.rtmpServer = vertx.createNetServer(NetServerOptions(tcpNoDelay = true, usePooledBuffers = true))
    val netClient = vertx.createNetClient()
    this.rtmpServer.connectHandler {
      it.pause()
      DefaultSwapper(it, netClient, namespaceManager, securityManager)
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
    private val KEY_NS = "ns"
    private val KEY_TK = "tk"
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private fun consumeAsJSON(ctx: RoutingContext, ob: Single<*>, statusCode: Int = 200) {
      ob.subscribeOn(Schedulers.io()).subscribe({
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader(Consts.HEADER_CONTENT_TYPE, Consts.CONTENT_TYPE_JSON_UTF8)
            .end(Utils.toJSON(it))
      }, { ex ->
        val resp = ctx.response()
            .putHeader(Consts.HEADER_CONTENT_TYPE, Consts.CONTENT_TYPE_JSON_UTF8)
        var code: Int = 500
        var ecode: Int = Errors.unknown
        var msg: String? = null
        ex.message?.let { msg = it }
        when (ex) {
          is RaptorException -> {
            val c = ex.toCode()
            code = c.second
            ecode = c.first
          }
          is IllegalArgumentException -> code = 400
        }
        resp.setStatusCode(code).end(Shit(ecode, msg).toString())
      })
    }

  }

}