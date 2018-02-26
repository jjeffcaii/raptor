# Raptor
Raptor是一个用Kotlin实现的动态RTMP直播推流代理库。您可以集成它来实现多端同时直播推流。[Engish Docs](./README.md)

## 样例代码
> 请参考: raptor-server/src/test/kotlin/me/zarafa/raptor/server/Example.kt
```kotlin
package me.zarafa.raptor.server

import me.zarafa.raptor.api.Address
import me.zarafa.raptor.api.ChannelManager

object Example {

  // 这个是一个Mock用的ChannelManager, 您可以根据您的业务自行实现该接口。
  private val channels: ChannelManager = ExampleChannelManager({ _ -> "?k=12345678" })
  private val server: RTMPProxyServer

  init {
    // 使用右侧地址来进行推流 => rtmp://127.0.0.1:1935/test?k=12345678
    // 如果你像我一样使用OBS, 那么请在设置里这样填写: URL => rtmp://127.0.0.1:1935/test, StreamKey => ?k=12345678
    // 如果一切正常, 那么恭喜你, 你的视频流应该会同时推送到upstream1和upstream2。
    // ***** 注意: 下面的直播推送地址是我测试用的, 请替换为您自己的推流地址!!! *****
    val upstream1 = "rtmp://pili-publish.maxwon.cn/maxwon-live/foo?e=1491469885&token=Thphesb5UQHYEMKQspI4LrUUKO3gWd47rEvGdHcK:j3cKLk84CYPx3koCQru6jlLoRO4="
    val upstream2 = "rtmp://pili-publish.maxwon.cn/maxwon-live/bar?e=1491469885&token=Thphesb5UQHYEMKQspI4LrUUKO3gWd47rEvGdHcK:j3cKLk84CYPx3koCQru6jlLoRO4="
    this.channels.save("test", arrayOf(Address.from(upstream1)!!, Address.from(upstream2)!!))
    this.server = RTMPProxyServer(this.channels)
  }

  @JvmStatic
  fun main(args: Array<String>) {
    this.server.run()
  }
}
```

## 时序图
![time_sequence_diagram](docs/timeseq.png "time_sequence")

## 类似的项目
 - https://github.com/arut/nginx-rtmp-module
