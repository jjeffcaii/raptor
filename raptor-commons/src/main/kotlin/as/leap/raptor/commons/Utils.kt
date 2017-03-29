package `as`.leap.raptor.commons

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.ByteArrayOutputStream
import java.io.InputStream


object Utils {

  private val mapper = ObjectMapper().registerModule(KotlinModule())

  fun toJSON(obj: Any): String {
    ByteArrayOutputStream().use {
      val factory = mapper.factory.createGenerator(it, JsonEncoding.UTF8)
      factory.writeObject(obj)
      return String(it.toByteArray(), Charsets.UTF_8)
    }
  }

  fun <T> fromJSON(json: String, clazz: Class<T>): T {
    return mapper.readValue(json, clazz)
  }

  fun <T> fromJSON(json: InputStream, clazz: Class<T>): T {
    return mapper.readValue(json, clazz)
  }


  fun <T> fromJSONArray(json: String, clazz: Class<T>): Array<T> {
    return mapper.readValue(json, TypeFactory.defaultInstance().constructArrayType(clazz))
  }


}