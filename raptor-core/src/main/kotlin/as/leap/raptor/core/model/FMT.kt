package `as`.leap.raptor.core.model

enum class FMT(val code: Int) {
  _0(0),
  _1(1),
  _2(2),
  _3(3);

  companion object {
    fun valueOf(code: Byte): FMT {
      val c = (code.toInt() and 0xFF).ushr(6)
      return when (c) {
        0 -> _0
        1 -> _1
        2 -> _2
        3 -> _3
        else -> throw UnsupportedOperationException("Not valid FMT: byte=$code, fmt=$c.")
      }
    }
  }


}