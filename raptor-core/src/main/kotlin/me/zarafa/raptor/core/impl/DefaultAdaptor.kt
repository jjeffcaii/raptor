package me.zarafa.raptor.core.impl

import io.vertx.core.net.NetClient
import me.zarafa.raptor.api.Address
import me.zarafa.raptor.commons.Consts
import me.zarafa.raptor.core.Adaptor
import me.zarafa.raptor.core.model.*
import me.zarafa.raptor.core.model.payload.*
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class DefaultAdaptor(
    netClient: NetClient,
    address: Address,
    chunkSize: Long = Consts.RTMP_DEFAULT_CHUNK_SIZE,
    reconnect: Int = 0
) : Adaptor(netClient, address, chunkSize, reconnect) {

  private var mark: Int = -1

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
        val header = Header(FMT.F1, 3, MessageType.COMMAND_AMF0)
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
        this.mark = this.transId++
        payload = CommandCreateStream(this.mark, arrayOfNulls(1))
        this.write(header, payload)
        if (logger.isDebugEnabled) {
          logger.debug(">>> create stream.")
        }
      }
      else -> {
        if (cmd.transId == this.mark) {
          val payload = CommandPublish(this.transId++, arrayOf(null, this.address.key, "live"))
          val header = Header(FMT.F0, msg.header.csid + 1, MessageType.COMMAND_AMF0, 0, 0L, 1L)
          this.write(header, payload)
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