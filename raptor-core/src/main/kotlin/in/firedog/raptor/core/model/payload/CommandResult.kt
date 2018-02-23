package `in`.firedog.raptor.core.model.payload

class CommandResult(transId: Int, values: Array<Any?>) : AbstractCommand(NAME, transId, values) {

  fun getInfo(key: String): Any? {
    if (this.values.size < 2) {
      return null
    }
    var ret: Any? = null
    this.values[1].let {
      if (it is Map<*, *>) {
        ret = it[key]
      }
    }
    return ret
  }

  companion object {
    val NAME = "_result"
  }

}