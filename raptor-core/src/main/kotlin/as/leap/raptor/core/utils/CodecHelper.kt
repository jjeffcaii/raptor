package `as`.leap.raptor.core.utils

import com.google.common.hash.Hashing

object CodecHelper {

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

  fun decodeAMF0(bytes: ByteArray) {

  }

}