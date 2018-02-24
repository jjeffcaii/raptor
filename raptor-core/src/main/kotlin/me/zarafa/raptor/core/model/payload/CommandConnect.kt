package me.zarafa.raptor.core.model.payload

import flex.messaging.io.amf.ASObject
import org.apache.commons.collections4.MapUtils
import org.apache.commons.lang3.StringUtils

class CommandConnect(transId: Int, objects: Array<Any?>) : AbstractCommand(NAME, transId, objects) {

  fun getConnectInfo(): ConnectInfo {
    val m = this.values.first() as ASObject
    val app = MapUtils.getString(m, "app", StringUtils.EMPTY)
    val flashver = MapUtils.getString(m, "flashver")
    val swfUrl = MapUtils.getString(m, "swfUrl")
    val tcUrl = MapUtils.getString(m, "tcUrl")
    val fpad = MapUtils.getBoolean(m, "fpad", null)
    val ac = MapUtils.getInteger(m, "audioCodecs")
    val vc = MapUtils.getInteger(m, "videoCodecs")
    val vf = MapUtils.getInteger(m, "videoFunction")
    val pageUrl = MapUtils.getString(m, "pageUrl")
    val objEnc = MapUtils.getInteger(m, "objectEncoding")
    return ConnectInfo(app, flashver, swfUrl, tcUrl, fpad, ac, vc, vf, pageUrl, objEnc)
  }

  companion object {
    val NAME = "connect"
  }

  data class ConnectInfo(
      var app: String,
      var flashver: String?,
      var swfUrl: String?,
      var tcUrl: String?,
      var fpad: Boolean?,
      var audioCodecs: Int?,
      var videoCodecs: Int?,
      var videoFunction: Int?,
      var pageUrl: String?,
      var objectEncoding: Int?
  )

}