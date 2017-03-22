package `as`.leap.raptor.api

interface NamespaceManager {

  fun exists(namespace: String): Boolean

  fun address(namespace: String, streamKey: String): Array<Address>

  companion object {

    val INSTANCE: NamespaceManager = object : NamespaceManager {

      override fun address(namespace: String, streamKey: String): Array<Address> {
        val qiniu = Address(
            Address.Provider.QINIU,
            "pili-publish.maxwon.cn",
            "maxwon-live",
            "/foobar?e=1490184723&token=3z_CkMd7_hhzf8XSYTmAtLM7FP0Nldr_lyleI-X9:yfY0GpfK7fIEHJmqdV9bpO6nyX4="
        )
        return arrayOf(qiniu)
      }

      override fun exists(namespace: String): Boolean {
        return true
      }
    }

  }

}