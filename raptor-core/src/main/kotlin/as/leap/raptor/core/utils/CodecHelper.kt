package `as`.leap.raptor.core.utils

import com.google.common.hash.Hashing
import flex.messaging.io.SerializationContext
import flex.messaging.io.amf.AbstractAmfInput
import flex.messaging.io.amf.Amf0Input
import flex.messaging.io.amf.Amf3Input
import java.io.ByteArrayInputStream

object CodecHelper {

  private val AMF_CONTEXT by lazy {
    SerializationContext()
  }

  fun murmur128(bytes: ByteArray): String {
    return Hashing.murmur3_128().newHasher()
        .putBytes(bytes)
        .hash()
        .toString()
  }

  fun murmur32(bytes: ByteArray): String {
    return Hashing.murmur3_32().newHasher()
        .putBytes(bytes)
        .hash()
        .toString()
  }

  fun decodeAMF0(bytes: ByteArray): List<Any> {
    return this.decodeAMF(Amf0Input(AMF_CONTEXT), bytes)
  }

  fun decodeAMF3(bytes: ByteArray): List<Any> {
    return this.decodeAMF(Amf3Input(AMF_CONTEXT), bytes)
  }

  private fun decodeAMF(deserializer: AbstractAmfInput, bytes: ByteArray): List<Any> {
    ByteArrayInputStream(bytes).use {
      deserializer.setInputStream(it)
      val li = mutableListOf<Any>()
      do {
        val obj = deserializer.readObject()
        li.add(obj ?: Unit)
      } while (deserializer.available() > 0)
      return li
    }
  }

}