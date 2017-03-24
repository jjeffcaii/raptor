package `as`.leap.raptor.core.adaptor

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.core.Adaptor
import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.payload.*
import `as`.leap.raptor.core.utils.Do
import io.vertx.core.buffer.Buffer
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class QiniuAdaptor(address: Address, chunkSize: Long, onConnect: Do? = null, onClose: Do?) : Adaptor(address, chunkSize, onConnect, onClose) {

  private var tidOfCreateStream: Int = -1

  override fun onCommand(msg: Message) {
    val cmd = msg.toModel()
    when (cmd) {
      is CommandResult -> {
        val c = cmd.getInfo("code")
        val code = if (c == null) {
          StringUtils.EMPTY
        } else {
          c as String
        }
        when (code) {
          "NetConnection.Connect.Success" -> {
            // snd release stream
            var payload: Payload = CommandReleaseStream(cmd.transId + 1, arrayOf(null, address.key))
            var b: Buffer = payload.toBuffer()
            val header: Header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0, b.length())
            backend.write(header.toBuffer().appendBuffer(b))
            if (logger.isDebugEnabled) {
              logger.debug(">>> release stream.")
            }
            // snd FCPublish
            payload = CommandFCPublish(cmd.transId + 2, arrayOf(null, address.key))
            b = payload.toBuffer()
            header.fmt = FMT.F1
            header.csid = msg.header.csid
            header.length = b.length()
            backend.write(header.toBuffer().appendBuffer(b))
            if (logger.isDebugEnabled) {
              logger.debug(">>> FCPublish.")
            }
            // snd create stream
            this.tidOfCreateStream = cmd.transId + 3
            payload = CommandCreateStream(this.tidOfCreateStream, arrayOfNulls(1))
            b = payload.toBuffer()
            header.length = b.length()
            backend.write(header.toBuffer().appendBuffer(b))
            if (logger.isDebugEnabled) {
              logger.debug(">>> create stream.")
            }
          }
          else -> {
            when (cmd.transId) {
              this.tidOfCreateStream -> {
                val payload = CommandPublish(this.tidOfCreateStream + 1, arrayOf(null, this.address.key, "live"))
                val b = payload.toBuffer()
                val header = Header(FMT.F0, msg.header.csid + 1, MessageType.COMMAND_AMF0, b.length(), 0L, 1L)
                backend.write(header.toBuffer().appendBuffer(b))
              }
              else -> {
                //TODO
              }
            }
          }
        }
      }
      is CommandOnStatus -> {
        this.connected.set(true)
        this.onConnect?.invoke()
      }
      else -> {
        //TODO
      }
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }


}