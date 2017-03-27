package `as`.leap.raptor.commons

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.type.TypeFactory
import java.io.ByteArrayOutputStream


object Utils {

  private val objectMapper: ObjectMapper by lazy {
    val foo = ObjectMapper()
    foo.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    foo.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    foo.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
    foo
  }

  fun toJSON(obj: Any): String {
    ByteArrayOutputStream().use {
      val factory = objectMapper.factory.createGenerator(it, JsonEncoding.UTF8)
      factory.writeObject(obj)
      return String(it.toByteArray(), Charsets.UTF_8)
    }
  }

  fun <T> fromJSON(json: String, clazz: Class<T>): T {
    return objectMapper.readValue(json, clazz)
  }

  fun <T> fromJSONArray(json: String, clazz: Class<T>): Array<T> {
    return objectMapper.readValue(json, TypeFactory.defaultInstance().constructArrayType(clazz))
  }


}