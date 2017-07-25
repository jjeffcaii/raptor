package `as`.leap.raptor.core

interface Storage {

  fun set(key: ByteArray, value: ByteArray)

  fun get(key: ByteArray): ByteArray

  fun del(key: ByteArray)

}