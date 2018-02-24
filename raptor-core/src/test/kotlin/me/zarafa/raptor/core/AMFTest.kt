package me.zarafa.raptor.core

import me.zarafa.raptor.core.utils.Codecs
import flex.messaging.io.SerializationContext
import flex.messaging.io.amf.Amf0Input
import flex.messaging.io.amf.Amf0Output
import org.testng.Assert
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class AMFTest {

  private val context = SerializationContext()

  @Test
  fun test() {
    var bytes: ByteArray? = null
    val arr = arrayOf("_result", 1, mapOf("foo" to "bar"), mapOf("bar" to "foo"))
    ByteArrayOutputStream().use {
      val serializer = Amf0Output(this.context)
      serializer.setOutputStream(it)
      arr.forEach {
        serializer.writeObject(it)
      }
      it.flush()
      bytes = it.toByteArray()
    }
    Assert.assertNotNull(bytes)
    println(Codecs.encodeHex(bytes!!, true))
    Assert.assertTrue(bytes!!.isNotEmpty())

    ByteArrayInputStream(bytes).use {
      val des = Amf0Input(this.context)
      des.setInputStream(it)
      val li = mutableListOf<Any?>()
      do {
        li.add(des.readObject())
      } while (des.available() > 0)
      println(li)
    }

  }

}