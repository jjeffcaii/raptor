package `as`.leap.raptor.core.impl

import `as`.leap.raptor.api.Address
import `as`.leap.raptor.core.Adaptor
import `as`.leap.raptor.core.model.*
import `as`.leap.raptor.core.model.payload.*
import `as`.leap.raptor.core.utils.Do
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class DefaultAdaptor(address: Address, chunkSize: Long, onConnect: Do? = null, onClose: Do?) : Adaptor(address, chunkSize, onConnect, onClose) {

  private var vipTransId: Int = -1

  private fun handleResultCommand(msg: Message, cmd: CommandResult) {
    val c = cmd.getInfo("code")
    val code = if (c == null) {
      StringUtils.EMPTY
    } else {
      c as String
    }

    when (code) {
      CONNECT_SUCCESS -> {
        // snd release stream
        val header: Header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
        var payload: Payload = CommandReleaseStream(this.transId++, arrayOf(null, address.key))
        this.write(header, payload)
        if (logger.isDebugEnabled) {
          logger.debug(">>> release stream.")
        }
        // snd FCPublish
        payload = CommandFCPublish(this.transId++, arrayOf(null, address.key))
        header.fmt = FMT.F1
        header.csid = msg.header.csid
        this.write(header, payload)
        if (logger.isDebugEnabled) {
          logger.debug(">>> FCPublish.")
        }
        // snd create stream
        this.vipTransId = this.transId++
        payload = CommandCreateStream(this.vipTransId, arrayOfNulls(1))
        this.write(header, payload)
        if (logger.isDebugEnabled) {
          logger.debug(">>> create stream.")
        }
      }
      else -> {
        when (cmd.transId) {
          this.vipTransId -> {
            val payload = CommandPublish(this.transId++, arrayOf(null, this.address.key, "live"))
            val header = Header(FMT.F0, msg.header.csid + 1, MessageType.COMMAND_AMF0, 0, 0L, 1L)
            this.write(header, payload)
          }
          else -> {
            // ignore other results
          }
        }
      }
    }
  }

  override fun onCommand(msg: Message) {
    val cmd = msg.toModel()
    when (cmd) {
      is CommandResult -> this.handleResultCommand(msg, cmd)
      is CommandOnStatus -> this.ok()
      else -> {
        logger.info("rcv other commands: {}", cmd)
        //TODO handle other commands.
      }
    }
  }

  companion object {
    private val CONNECT_SUCCESS = "NetConnection.Connect.Success"
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }


}