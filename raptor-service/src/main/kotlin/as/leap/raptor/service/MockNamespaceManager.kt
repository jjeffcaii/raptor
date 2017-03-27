package `as`.leap.raptor.service

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import com.qiniu.pili.Client

class MockNamespaceManager : NamespaceManager {

  private val client: Client = Client("Thphesb5UQHYEMKQspI4LrUUKO3gWd47rEvGdHcK", "qms507cIEplAN85Phul_EincA0Jatp1l0BdBNFRJ")

  override fun clear(namespace: String, group: String) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun set(namespace: String, group: String, addresses: Array<Address>, expiresInSeconds: Int) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun address(namespace: String, group: String): Array<Address> {
    val s = this.client.RTMPPublishURL("pili-publish.maxwon.cn", "maxwon-live", "foobar", 60)
    return arrayOf(
        Address("send3.douyu.com", namespace, group),
        Address("pili-publish.maxwon.cn", "maxwon-live", s.substring(s.lastIndexOf("/"))),
        Address("dl.live-send.acg.tv", "live-dl", "?streamname=live_94338558_9096008&key=b7e4081cbfb773a2d45d43888fcacb16")
    )
  }

  override fun exists(namespace: String): Boolean {
    return true
  }
}