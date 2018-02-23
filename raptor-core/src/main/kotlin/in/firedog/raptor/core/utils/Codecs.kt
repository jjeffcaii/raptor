package `in`.firedog.raptor.core.utils

import com.google.common.base.Splitter
import com.google.common.hash.Hashing
import flex.messaging.io.SerializationContext
import flex.messaging.io.amf.*
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.invoke.MethodHandles
import java.util.regex.Pattern

object Codecs {

  private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

  private val AMF_CONTEXT by lazy {
    SerializationContext()
  }

  private val SPACE_PATTERN by lazy {
    Pattern.compile("\\s+")
  }

  fun md5(bytes: ByteArray): String {
    return DigestUtils.md5Hex(bytes)
  }

  fun decodeHex(str: String, removeSpace: Boolean = false): ByteArray {
    if (!removeSpace) {
      return Hex.decodeHex(str.toCharArray())
    }
    val sb = StringBuffer(str.length)
    Splitter.on(SPACE_PATTERN).split(str).forEach {
      sb.append(it)
    }
    return decodeHex(sb.toString())
  }

  fun encodeHex(bytes: ByteArray, pretty: Boolean = false): String {
    if (bytes.isEmpty()) {
      return StringUtils.EMPTY
    }
    if (!pretty) {
      return Hex.encodeHexString(bytes)
    }
    val arr = Hex.encodeHexString(bytes).toCharArray()
    val sb = StringBuffer()
    for (i in 1 until arr.size) {
      sb.append(arr[i - 1])
      when {
        i and 31 == 0 -> sb.append(StringUtils.LF)
        i and 15 == 0 -> sb.append(StringUtils.SPACE).append(StringUtils.SPACE)
        i and 1 == 0 -> sb.append(StringUtils.SPACE)
      }
    }
    sb.append(arr.last())
    return sb.toString()
  }

  fun encodeBase64(bytes: ByteArray): String {
    return Base64.encodeBase64String(bytes)
  }

  fun decodeBase64(str: String): ByteArray {
    return Base64.decodeBase64(str)
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

  fun encodeAMF0(values: Array<Any?>): ByteArray {
    ByteArrayOutputStream().use {
      val serializer = Amf0Output(AMF_CONTEXT)
      serializer.setOutputStream(it)
      values.forEach { serializer.writeObject(it) }
      it.flush()
      return it.toByteArray()
    }
  }

  fun encodeAMF3(values: Array<Any?>): ByteArray {
    ByteArrayOutputStream().use {
      val serializer = Amf3Output(AMF_CONTEXT)
      serializer.setOutputStream(it)
      values.forEach { serializer.writeObject(it) }
      it.flush()
      return it.toByteArray()
    }
  }

  fun decodeAMF0(bytes: ByteArray): Array<Any?> {
    try {
      return this.decodeAMF(Amf0Input(AMF_CONTEXT), bytes)
    } catch (e: Throwable) {
      logger.error("decode as amf0 failed.", e)
      throw e
    }
  }

  fun decodeAMF3(bytes: ByteArray): Array<Any?> {
    return this.decodeAMF(Amf3Input(AMF_CONTEXT), bytes)
  }

  private fun decodeAMF(deserializer: AbstractAmfInput, bytes: ByteArray): Array<Any?> {
    ByteArrayInputStream(bytes).use {
      deserializer.setInputStream(it)
      val li = mutableListOf<Any?>()
      do {
        li.add(deserializer.readObject())
      } while (deserializer.available() > 0)
      return li.toTypedArray()
    }
  }
}