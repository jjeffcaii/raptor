package `as`.leap.raptor.core

import `as`.leap.raptor.core.utils.CodecHelper
import flex.messaging.io.SerializationContext
import flex.messaging.io.amf.Amf0Input
import flex.messaging.io.amf.Amf3Input
import flex.messaging.io.amf.Amf3Output
import io.vertx.core.buffer.Buffer
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.experimental.and


class FoobarTest {

  @Test
  fun test() {
    val buffer = Buffer.buffer(1)
    buffer.appendByte(0x1f)
    buffer.appendByte(0x1f)
    println(buffer.length())
  }

  @Test
  fun test2() {

    val b: Byte = 0x02

    val bb = b and 0x3F

    println(bb)
  }

  @Test
  fun test3() {
    val b64 = "AgAHY29ubmVjdAA/8AAAAAAAAAMAA2FwcAIAa21heHdvbi1saXZlL2Zvb2Jhcj9lPTE0ODg3ODQ1ODcmdG9rZW49ekhKcmpqd2NjQl9uMzBPMTZDQWl4THZmSl8wYnMwZ2Z0WnY4b09vSDpQLTJobEdGMDZjRzVteVNEZG5EcjBsMkhMVWs9AAR0eXBlAgAKbm9ucHJpdmF0ZQAIZmxhc2hWZXICAB9GTUxFLzMuMCAoY29tcGF0aWJsZTsgRk1TYy8xLjApAAZzd2ZVcmwCAHxydG1wOi8vMTI3LjAuMC4xL21heHdvbi1saXZlL2Zvb2Jhcj9lPTE0ODg3ODQ1ODcmdG9rZW49ekhKcmpqd2NjQl9uMzBPMTZDQWl4THZmSl8wYnMwZ2Z0WnY4b09vSDpQLTJobEdGMDZjRzVteVNEZG5EcjBsMkhMVWs9AAV0Y1VybAIAfHJ0bXA6Ly8xMjcuMC4wLjEvbWF4d29uLWxpdmUvZm9vYmFyP2U9MTQ4ODc4NDU4NyZ0b2tlbj16SEpyamp3Y2NCX24zME8xNkNBaXhMdmZKXzBiczBnZnRadjhvT29IOlAtMmhsR0YwNmNHNW15U0RkbkRyMGwySExVaz0AAAk="
    val bytes = Base64.getDecoder().decode(b64)
    val context = SerializationContext()

    val model = TestModel(123, "foobar")

    val serializer = Amf3Output(context)
    val bos = ByteArrayOutputStream()
    serializer.setOutputStream(bos)
    serializer.writeObject(model)
    serializer.close()

    val bytes2 = bos.toByteArray()
    val deserializer = Amf3Input(context)
    val bis = ByteArrayInputStream(bytes2)
    deserializer.setInputStream(bis)
    val o = deserializer.readObject()
    println(o)
    deserializer.close()


  }

  @Test
  fun test4() {
    val b64 = "AgAHY29ubmVjdAA/8AAAAAAAAAMAA2FwcAIAa21heHdvbi1saXZlL2Zvb2Jhcj9lPTE0ODg3ODQ1ODcmdG9rZW49ekhKcmpqd2NjQl9uMzBPMTZDQWl4THZmSl8wYnMwZ2Z0WnY4b09vSDpQLTJobEdGMDZjRzVteVNEZG5EcjBsMkhMVWs9AAR0eXBlAgAKbm9ucHJpdmF0ZQAIZmxhc2hWZXICAB9GTUxFLzMuMCAoY29tcGF0aWJsZTsgRk1TYy8xLjApAAZzd2ZVcmwCAHxydG1wOi8vMTI3LjAuMC4xL21heHdvbi1saXZlL2Zvb2Jhcj9lPTE0ODg3ODQ1ODcmdG9rZW49ekhKcmpqd2NjQl9uMzBPMTZDQWl4THZmSl8wYnMwZ2Z0WnY4b09vSDpQLTJobEdGMDZjRzVteVNEZG5EcjBsMkhMVWs9AAV0Y1VybAIAfHJ0bXA6Ly8xMjcuMC4wLjEvbWF4d29uLWxpdmUvZm9vYmFyP2U9MTQ4ODc4NDU4NyZ0b2tlbj16SEpyamp3Y2NCX24zME8xNkNBaXhMdmZKXzBiczBnZnRadjhvT29IOlAtMmhsR0YwNmNHNW15U0RkbkRyMGwySExVaz0AAAk="
    val bytes = Base64.getDecoder().decode(b64)
    val context = SerializationContext()

    val deserializer = Amf0Input(context)
    val bis = ByteArrayInputStream(bytes)
    deserializer.setInputStream(bis)
    println("av:" + deserializer.available())
    println(deserializer.readObject())
    println(deserializer.readObject())
    println(deserializer.readObject())
    println("av:" + deserializer.available())
    println(deserializer.readObject())
    deserializer.close()


  }

  @Test
  fun test5() {
    val b64 = "AgAHY29ubmVjdAA/8AAAAAAAAAMAA2FwcAIAa21heHdvbi1saXZlL2Zvb2Jhcj9lPTE0ODg3ODQ1ODcmdG9rZW49ekhKcmpqd2NjQl9uMzBPMTZDQWl4THZmSl8wYnMwZ2Z0WnY4b09vSDpQLTJobEdGMDZjRzVteVNEZG5EcjBsMkhMVWs9AAR0eXBlAgAKbm9ucHJpdmF0ZQAIZmxhc2hWZXICAB9GTUxFLzMuMCAoY29tcGF0aWJsZTsgRk1TYy8xLjApAAZzd2ZVcmwCAHxydG1wOi8vMTI3LjAuMC4xL21heHdvbi1saXZlL2Zvb2Jhcj9lPTE0ODg3ODQ1ODcmdG9rZW49ekhKcmpqd2NjQl9uMzBPMTZDQWl4THZmSl8wYnMwZ2Z0WnY4b09vSDpQLTJobEdGMDZjRzVteVNEZG5EcjBsMkhMVWs9AAV0Y1VybAIAfHJ0bXA6Ly8xMjcuMC4wLjEvbWF4d29uLWxpdmUvZm9vYmFyP2U9MTQ4ODc4NDU4NyZ0b2tlbj16SEpyamp3Y2NCX24zME8xNkNBaXhMdmZKXzBiczBnZnRadjhvT29IOlAtMmhsR0YwNmNHNW15U0RkbkRyMGwySExVaz0AAAk="
    val bytes = Base64.getDecoder().decode(b64)

    var i = 0
    CodecHelper.decodeAMF0(bytes).forEach {
      println("${i++}: $it")
    }


  }

  @Test
  fun test6() {



  }


}