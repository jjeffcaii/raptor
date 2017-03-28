package `as`.leap.raptor.service

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import com.google.common.base.Splitter
import com.qiniu.pili.Client
import org.apache.commons.lang3.StringUtils

class SimpleNamespaceManager : NamespaceManager {
  override fun clear(namespace: String, group: String) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun save(namespace: String, group: String, addresses: Array<Address>, expiresInSeconds: Int) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }


  private val client: Client = Client("Thphesb5UQHYEMKQspI4LrUUKO3gWd47rEvGdHcK", "qms507cIEplAN85Phul_EincA0Jatp1l0BdBNFRJ")

  override fun load(namespace: String, group: String): Array<Address> {
    return Splitter.on(",").split(StringUtils.stripStart(group, "/"))
        .map {
          val s = this.client.RTMPPublishURL(domain, namespace, it, 60)
          Address(domain, namespace, s.substring(s.lastIndexOf("/")))
        }
        .toTypedArray()
  }

  override fun exists(namespace: String, group: String): Boolean {
    return true
  }

  companion object {
    private val domain = "pili-publish.maxwon.cn"
  }
}