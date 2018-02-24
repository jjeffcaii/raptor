package me.zarafa.raptor.server

import me.zarafa.raptor.api.Address
import me.zarafa.raptor.api.NamespaceManager
import me.zarafa.raptor.commons.Consts
import me.zarafa.raptor.commons.Errors
import me.zarafa.raptor.commons.Utils
import me.zarafa.raptor.core.impl.DefaultSwapper
import me.zarafa.raptor.server.vo.ErrorMessage
import me.zarafa.raptor.server.vo.GroupVO
import me.zarafa.raptor.server.vo.PostAddress
import me.zarafa.raptor.server.vo.PostGroup
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
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.invoke.MethodHandles

class RaptorServer(private val namespaceManager: NamespaceManager, private val opts: RaptorOptions) : Runnable {
  private val vertx = Vertx.vertx()
  private val apiServer: HttpServer
  private val rtmpServer: NetServer

  init {
    // create restful server.
    this.apiServer = vertx.createHttpServer()
    val router = Router.router(this.vertx)
    opts.www?.let {
      val f = File(it)
      if (f.exists() && f.isDirectory) {
        if (logger.isDebugEnabled) {
          logger.debug("page files found: {}.", f.absolutePath)
        }
        router.route("/console/*").handler(StaticHandler.create(it))
        router.get("/favicon.ico").handler { ctx ->
          ctx.response().sendFile(arrayOf(it, "favicon.ico").joinToString("/"))
        }
      }
    }

    router.route().handler(BodyHandler.create())

    // enable cross domain.
    router.route().handler {
      it.response().putHeader("Access-Control-Allow-Origin", "*")
      when (it.request().method()) {
        HttpMethod.OPTIONS -> it.response().end()
        else -> it.next()
      }
    }

    // permission check.
    router.route().handler { ctx ->
      val ns = ctx.request().getHeader(Consts.HEADER_MAXLEAP_APPID)
      if (ns.isBlank()) {
        ctx.response()
            .putHeader(Consts.HEADER_CONTENT_TYPE, Consts.CONTENT_TYPE_JSON_UTF8)
            .setStatusCode(401)
            .end(ErrorMessage(Errors.security, "Not valid APP_ID or API_KEY!").toString())
      } else {
        ctx.put(KEY_NS, ns).next()
      }
    }

    router.post("/groups/:gp").consumes(Consts.CONTENT_TYPE_JSON).handler { ctx ->
      val ob = Single.create<Int> {
        val ns: String = ctx.get(KEY_NS)
        val gp = ctx.request().getParam("gp")
        val vo = Utils.fromJSON(ctx.bodyAsString, PostGroup::class.java)
        val addresses = vo.addresses.map { it.toAddress()!! }.toList().toTypedArray()
        if (this.namespaceManager.exists(ns, gp)) {
          throw Exception("group $gp exists already!")
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
          else -> throw Exception("no such group $gp.")
        }
      }
      consumeAsJSON(ctx, ob)
    }

    router.get("/groups/:gp/publish").handler { ctx ->
      val ob = Single.create<Any> {
        val ns: String = ctx.get(KEY_NS)
        val gp = ctx.request().getParam("gp")
        when (this.namespaceManager.exists(ns, gp)) {
          true -> {
            val url = when (opts.rtmpPort) {
              Address.DEFAULT_PORT -> "rtmp://${opts.hostname}/$ns?g=$gp"
              else -> "rtmp://${opts.hostname}:${opts.rtmpPort}/$ns?g=$gp"
            }
            it.onSuccess(mapOf("url" to url))
          }
          else -> throw Exception("no such group $gp.")
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
      DefaultSwapper(it, netClient, opts.strategy, opts.reconnect, namespaceManager)
      it.resume()
    }
  }

  override fun run() {
    this.apiServer.listen(this.opts.httpPort, {
      when (it.succeeded()) {
        true -> logger.info("API server start success! (http://127.0.0.1:${this.opts.httpPort}/console/)")
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
        var msg: String? = null
        ex.message?.let { msg = it }
        resp.setStatusCode(500).end(msg)
      })
    }

  }

}