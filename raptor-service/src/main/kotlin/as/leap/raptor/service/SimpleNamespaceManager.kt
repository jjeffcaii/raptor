package `as`.leap.raptor.service

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import com.google.common.base.Splitter
import com.qiniu.pili.Client
import org.apache.commons.lang3.StringUtils

class SimpleNamespaceManager : NamespaceManager {

  private val client: Client = Client("Thphesb5UQHYEMKQspI4LrUUKO3gWd47rEvGdHcK", "qms507cIEplAN85Phul_EincA0Jatp1l0BdBNFRJ")

  override fun address(namespace: String, streamKey: String): Array<Address> {
    return Splitter.on(",").split(StringUtils.stripStart(streamKey, "/"))
        .map {
          val s = this.client.RTMPPublishURL(domain, namespace, it, 60)
          Address(domain, namespace, s.substring(s.lastIndexOf("/")))
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