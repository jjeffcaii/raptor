package `as`.leap.raptor.api.impl

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.api.NamespaceManager
import `as`.leap.raptor.commons.Utils
import com.google.common.base.Splitter
import com.google.common.net.HostAndPort
import org.apache.commons.lang3.RandomStringUtils
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import redis.clients.jedis.JedisCluster

class NamespaceManagerTest {

  private var jedis: JedisCluster? = null
  private var manger: NamespaceManager? = null
  private val address = Address.from("rtmp://abc.com/ctx?k=1")

  @BeforeClass
  fun prepare() {
    val seeds = Splitter.on(",").splitToList("1.redis.cluster:6399,2.redis.cluster:6399,3.redis.cluster:6399")
        .map {
          val hp = HostAndPort.fromString(it)
          redis.clients.jedis.HostAndPort(hp.hostText, hp.getPortOrDefault(6399))
        }
        .toSet()
    this.jedis = JedisCluster(seeds)
    this.manger = NamespaceManagerImpl(this.jedis!!)
  }


  @Test
  fun testAddressJSON() {
    val json = Utils.toJSON(address!!)
    val address2 = Utils.fromJSON(json, Address::class.java)
    Assert.assertEquals(address, address2)
  }

  @Test
  fun test() {
    val ns = RandomStringUtils.random(24, "abcdef0123456789")
    val gp = "default"
    Assert.assertFalse(this.manger!!.exists(ns, gp))
    this.manger!!.save(ns, gp, arrayOf(this.address!!), 3)
    Assert.assertTrue(this.manger!!.exists(ns, gp))
    val groups = this.manger!!.list(ns)
    Assert.assertEquals(1, groups.size)
    Thread.sleep(1000)
    var addresses = this.manger!!.load(ns, gp)
    Assert.assertEquals(1, addresses.size)
    Thread.sleep(3000)
    addresses = this.manger!!.load(ns, gp)
    Assert.assertEquals(0, addresses.size)
    this.manger!!.clear(ns, gp)
  }

  @AfterClass
  fun teardown() {
    this.jedis!!.close()
  }


}