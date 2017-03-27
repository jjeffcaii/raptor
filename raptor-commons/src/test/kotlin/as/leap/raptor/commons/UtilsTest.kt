package `as`.leap.raptor.commons

import org.testng.Assert
import org.testng.annotations.Test

class UtilsTest {

  @Test
  fun testJson() {
    val map = mapOf("foo" to 1, "bar" to "qux")
    val json = Utils.toJSON(map)
    Assert.assertNotNull(json)
    println(json)
    val map2 = Utils.fromJSON(json, Map::class.java)
    println(map2)
    Assert.assertEquals(map, map2)
  }

}