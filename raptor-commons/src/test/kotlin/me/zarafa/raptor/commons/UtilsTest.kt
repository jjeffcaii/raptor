package me.zarafa.raptor.commons

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

  @Test
  fun test() {
    val str: String? = null
    println(str.isNullOrBlank())
  }

  @Test
  fun test2() {
    val s1 = String("abc".toCharArray())
    val s2 = String("abc".toCharArray())
    // value equals
    println(s1 == s2)
    // ref equals
    println(s1 === s2)
  }

}