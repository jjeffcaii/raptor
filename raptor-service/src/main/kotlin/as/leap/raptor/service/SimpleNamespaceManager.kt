package `as`.leap.raptor.service

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import com.google.common.base.Preconditions
import com.google.common.base.Splitter
import com.qiniu.pili.Client

class SimpleNamespaceManager : NamespaceManager {

  private val client: Client = Client("3z_CkMd7_hhzf8XSYTmAtLM7FP0Nldr_lyleI-X9", "jhR7vMVMGZmrYS5SGNBbMCYCvpfd8cE7GdT61mVS")

  override fun address(namespace: String, streamKey: String): Array<Address> {
    Preconditions.checkArgument(streamKey.startsWith("/"))
    return Splitter.on(",").split(streamKey.substring(1))
        .map {
          val s = this.client.RTMPPublishURL(domain, namespace, it, 60)
          Address(Address.Provider.QINIU, domain, namespace, s.substring(s.lastIndexOf("/")))
        }
        .toTypedArray()
  }

  override fun exists(namespace: String): Boolean {
    return "maxwon-live" == namespace
  }

  companion object {
    private val domain = "pili-publish.maxwon.cn"
  }
}