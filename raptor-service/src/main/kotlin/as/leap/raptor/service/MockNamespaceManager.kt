package `as`.leap.raptor.service

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import com.qiniu.pili.Client

class MockNamespaceManager : NamespaceManager {

  private val client: Client = Client("Thphesb5UQHYEMKQspI4LrUUKO3gWd47rEvGdHcK", "qms507cIEplAN85Phul_EincA0Jatp1l0BdBNFRJ")

  override fun address(namespace: String, streamKey: String): Array<Address> {
    val s = this.client.RTMPPublishURL("pili-publish.maxwon.cn", "maxwon-live", "foobar", 60)
    return arrayOf(
        Address("send3.douyu.com", namespace, streamKey),
        Address("pili-publish.maxwon.cn", "maxwon-live", s.substring(s.lastIndexOf("/"))),
        Address("dl.live-send.acg.tv", "live-dl", "?streamname=live_94338558_9096008&key=b7e4081cbfb773a2d45d43888fcacb16")
    )
  }

  override fun exists(namespace: String): Boolean {
    return true
  }
}