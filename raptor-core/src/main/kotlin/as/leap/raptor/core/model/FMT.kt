package `as`.leap.raptor.core.model

enum class FMT(val code: Int) {
  F1(0),
  F2(1),
  F3(2),
  F4(3);

  companion object {
    fun valueOf(code: Byte): FMT {
      val c = (code.toInt() and 0xFF).ushr(6)
      return when (c) {
        0 -> F1
        1 -> F2
        2 -> F3
        3 -> F4
        else -> throw UnsupportedOperationException("Not valid FMT: byte=$code, fmt=$c.")
      }
    }
  }


}