package me.zarafa.raptor.api

/**
 * manage channels
 */
interface ChannelManager {

  /**
   * check channel exists
   */
  fun exists(channel: String): Boolean

  /**
   * remove channel
   */
  fun remove(channel: String)

  /**
   * save channel
   */
  fun save(channel: String, addresses: Array<Address>, expiresInSeconds: Int = 60)

  /**
   * load channel
   */
  fun load(channel: String): Array<Address>

  /**
   * create stream key for channel
   */
  fun streamKey(channel: String): String

  /**
   * validate stream key
   */
  fun validate(channel: String, streamKey: String): Boolean
}