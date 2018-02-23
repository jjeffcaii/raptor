package `in`.firedog.raptor.core

import `in`.firedog.raptor.core.utils.Do
import java.io.Closeable

interface Pipe : Closeable {

  fun connect(cb: Do?): Pipe

  fun onClose(cb: Do?): Pipe

}