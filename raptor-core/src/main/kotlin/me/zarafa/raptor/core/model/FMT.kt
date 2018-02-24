package me.zarafa.raptor.core.model

enum class FMT(val code: Int) {
  F0(0),
  F1(1),
  F2(2),
  F3(3);

  companion object {
    fun valueOf(code: Byte): FMT {
      val c = (code.toInt() and 0xFF).ushr(6)
      return when (c) {
        0 -> F0
        1 -> F1
        2 -> F2
        3 -> F3
        else -> throw IllegalArgumentException("Not valid FMT: byte=$code, fmt=$c.")
      }
    }
  }


}