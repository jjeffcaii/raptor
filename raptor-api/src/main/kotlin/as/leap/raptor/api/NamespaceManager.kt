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
            "/foobar?e=1490244546&token=3z_CkMd7_hhzf8XSYTmAtLM7FP0Nldr_lyleI-X9:eroFC1dccr8bwmYXxdpI4BsrZFI="
        )
        return arrayOf(qiniu)
      }

      override fun exists(namespace: String): Boolean {
        return true
      }
    }

  }

}