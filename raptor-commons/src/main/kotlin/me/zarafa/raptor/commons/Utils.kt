package me.zarafa.raptor.commons

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.NetworkInterface
import java.util.regex.Pattern


object Utils {

  private val mapper: ObjectMapper by lazy {
    ObjectMapper().registerModule(KotlinModule())
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
  }

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

  fun ipv4(): List<String> {
    val PATTERN_IP4 = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")
    val li = mutableListOf<String>()
    NetworkInterface.getNetworkInterfaces().iterator().forEach { network ->
      network.inetAddresses.iterator().forEach {
        val addr = it.hostAddress
        if (PATTERN_IP4.matcher(addr).matches() && "127.0.0.1" != addr) {
          li.add(addr)
        }
      }
    }
    return li
  }

}