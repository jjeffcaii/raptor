package `as`.leap.raptor.service

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import com.qiniu.pili.Client

class MockNamespaceManager : NamespaceManager {

  private val client: Client = Client("Thphesb5UQHYEMKQspI4LrUUKO3gWd47rEvGdHcK", "qms507cIEplAN85Phul_EincA0Jatp1l0BdBNFRJ")

  override fun address(namespace: String, streamKey: String): Array<Address> {
    val s = this.client.RTMPPublishURL("pili-publish.maxwon.cn", "maxwon-live", "foobar", 60)
    return arrayOf(
        Address(Address.Provider.QINIU, "send3.douyu.com", namespace, streamKey),
        Address(Address.Provider.QINIU, "pili-publish.maxwon.cn", "maxwon-live", s.substring(s.lastIndexOf("/")))
    )
  }

  override fun exists(namespace: String): Boolean {
    return true
  }
}