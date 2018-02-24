package me.zarafa.raptor.commons

object Consts {

  val RTMP_DEFAULT_CHUNK_SIZE = 128L
  val RTMP_DEFAULT_CHUNK_SIZE32 = RTMP_DEFAULT_CHUNK_SIZE.toInt()

  val HEADER_MAXLEAP_APPID = "x-raptor-namespace"
  val HEADER_CONTENT_TYPE = "Content-Type"
  val CONTENT_TYPE_JSON = "application/json"
  val CONTENT_TYPE_JSON_UTF8 = "application/json; charset=utf-8"
}