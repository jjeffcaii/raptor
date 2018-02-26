package me.zarafa.raptor.server

import me.zarafa.raptor.api.Address
import me.zarafa.raptor.api.ChannelManager

class ExampleChannelManager(private val kenGen: (String) -> String) : ChannelManager {

  private val store = mutableMapOf<String, Array<Address>>()

  override fun exists(channel: String): Boolean {
    return this.store.containsKey(channel)
  }

  override fun remove(channel: String) {
    this.store.remove(channel)
  }

  override fun save(channel: String, addresses: Array<Address>) {
    this.store[channel] = addresses
  }

  override fun load(channel: String): Array<Address> {
    return this.store[channel] ?: emptyArray()
  }

  override fun streamKey(channel: String): String {
    return this.kenGen(channel)
  }

  override fun validate(channel: String, streamKey: String): Boolean {
    return streamKey == this.kenGen(channel)
  }

}