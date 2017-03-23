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
            "/foobar?e=1490255986&token=3z_CkMd7_hhzf8XSYTmAtLM7FP0Nldr_lyleI-X9:2XyRhwzMmneaeX3QUh5vR1Sfx48="
        )
        val qiniu2 = Address(
            Address.Provider.QINIU,
            "pili-publish.maxwon.cn",
            "maxwon-live",
            "/foo?e=1490256107&token=3z_CkMd7_hhzf8XSYTmAtLM7FP0Nldr_lyleI-X9:7zFsc0WRVObFNQMDJ3KzucVcMT0="
        )
        return arrayOf(qiniu, qiniu2)
      }

      override fun exists(namespace: String): Boolean {
        return true
      }
    }

  }

}