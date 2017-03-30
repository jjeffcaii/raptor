package `as`.leap.raptor.commons

object Consts {


  val HEADER_MAXLEAP_APPID = "X-ML-AppId"
  val HEADER_MAXLEAP_SIGN = "X-ML-Request-Sign"
  val HEADER_MAXLEAP_APIKEY = "X-ML-APIKey"
  val HEADER_CONTENT_TYPE = "Content-Type"
  val CONTENT_TYPE_JSON = "application/json"
  val CONTENT_TYPE_JSON_UTF8 = "application/json; charset=utf-8"

  val DEFAULT_GROUP_NAME = "default"
  val KEY_FOR_GROUP = "g"
  val KEY_FOR_SIGN = "k"

  val HEADER_CORS_ORIGIN = "Access-Control-Allow-Origin"
  val HEADER_CORS_METHOD = "Access-Control-Allow-Methods"
  val HEADER_CORS_HEADER = "Access-Control-Allow-Headers"

  val CORS_ORIGIN = "*"
  val CORS_METHOD = "HEAD, POST, GET, OPTIONS, DELETE, PUT"
  val CORS_HEADER = "Origin, X-Requested-With, Content-Type, Accept"
}