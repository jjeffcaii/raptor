package `as`.leap.raptor.core

import `as`.leap.raptor.core.utils.CodecHelper
import `as`.leap.raptor.core.utils.VertxHelper
import com.google.common.base.Splitter
import flex.messaging.io.SerializationContext
import flex.messaging.io.amf.Amf0Input
import flex.messaging.io.amf.Amf3Input
import flex.messaging.io.amf.Amf3Output
import io.vertx.core.buffer.Buffer
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.regex.Pattern
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
  fun test7() {
    val s = "AgAHX3Jlc3VsdAA/8AAAAAAAAAMABmZtc1ZlcgIADUZNUy8zLDAsMSwxMjMADGNhcGFiaWxpdGllcwBAPwAAAAAAAAAEbW9kZQA/8AAAAAAAAAAACQMABWxldmVsAgAGc3RhdHVzAARjb2RlAgAdTmV0Q29ubmVjdGlvbi5Db27DbmVjdC5TdWNjZXNzAAtkZXNjcmlwdGlvbgIAFUNvbm5lY3Rpb24gc3VjY2VlZGVkLgAOb2JqZWN0RW5jb2RpbmcAAAAAAAAAAAAABGRhdGEAAAAAAAAAAAAAB3ZlcnNpb24CAAkzLDUsMSw1MTYAAA=="
    val bytes = CodecHelper.decodeBase64(s)
    println(bytes.size)
    println(CodecHelper.encodeHex(bytes, true))
    //val values = CodecHelper.decodeAMF0(bytes)
    //println(values)
  }

  @Test
  fun test9() {
    val s = """
02 00 07 5f 72 65 73 75  6c 74 00 3f f0 00 00 00
00 00 00 03 00 06 66 6d  73 56 65 72 02 00 0d 46
4d 53 2f 33 2c 30 2c 31  2c 31 32 33 00 0c 63 61
70 61 62 69 6c 69 74 69  65 73 00 40 3f 00 00 00
00 00 00 00 04 6d 6f 64  65 00 3f f0 00 00 00 00
00 00 00 00 09 03 00 05  6c 65 76 65 6c 02 00 06
73 74 61 74 75 73 00 04  63 6f 64 65 02 00 1d 4e
65 74 43 6f 6e 6e 65 63  74 69 6f 6e 2e 43 6f 6e
6e 65 63 74 2e 53 75 63  63 65 73 73 00 0b 64 65
73 63 72 69 70 74 69 6f  6e 02 00 15 43 6f 6e 6e
65 63 74 69 6f 6e 20 73  75 63 63 65 65 64 65 64
2e 00 0e 6f 62 6a 65 63  74 45 6e 63 6f 64 69 6e
67 00 00 00 00 00 00 00  00 00 00 04 64 61 74 61
00 00 00 00 00 00 00 00  00 00 07 76 65 72 73 69
6f 6e 02 00 09 33 2c 35  2c 31 2c 35 31 36 00 00
09
"""

    val sb = StringBuffer()

    Splitter.on(Pattern.compile("\\s+")).split(s).forEach {
      sb.append(it)
    }
    val bytes = CodecHelper.decodeHex(sb.toString())
    println(CodecHelper.encodeHex(bytes, true))
    val values = CodecHelper.decodeAMF0(bytes)
    println(values)
  }

  @Test
  fun test10() {
    val b = Buffer.buffer(3)
    b.appendByte(1).appendByte(2).appendByte(3)

    val bb = b.slice(1, b.length())
    println(CodecHelper.encodeHex(b.bytes))
    println(CodecHelper.encodeHex(bb.bytes))


  }

  @Test
  fun test11() {
    val s = """
03 00 00 00 00 00 f1 14  00 00 00 00 02 00 07 5f
72 65 73 75 6c 74 00 3f  f0 00 00 00 00 00 00 03
00 06 66 6d 73 56 65 72  02 00 0d 46 4d 53 2f 33
2c 30 2c 31 2c 31 32 33  00 0c 63 61 70 61 62 69
6c 69 74 69 65 73 00 40  3f 00 00 00 00 00 00 00
04 6d 6f 64 65 00 3f f0  00 00 00 00 00 00 00 00
09 03 00 05 6c 65 76 65  6c 02 00 06 73 74 61 74
75 73 00 04 63 6f 64 65  02 00 1d 4e 65 74 43 6f
6e 6e 65 63 74 69 6f 6e  2e 43 6f 6e c3 6e 65 63
74 2e 53 75 63 63 65 73  73 00 0b 64 65 73 63 72
69 70 74 69 6f 6e 02 00  15 43 6f 6e 6e 65 63 74
69 6f 6e 20 73 75 63 63  65 65 64 65 64 2e 00 0e
6f 62 6a 65 63 74 45 6e  63 6f 64 69 6e 67 00 00
00 00 00 00 00 00 00 00  04 64 61 74 61 00 00 00
00 00 00 00 00 00 00 07  76 65 72 73 69 6f 6e 02
00 09 33 2c 35 2c 31 2c  35 31 36 00 00
"""
    val bytes = CodecHelper.decodeHex(s, true)
    println(bytes.size)
  }

  @Test
  fun testsss() {
    val b = Buffer.buffer(23)
    //VertxHelper.fillZero(b, 23)
    println(b.length())
    println(CodecHelper.encodeHex(b.bytes, true))

  }


}