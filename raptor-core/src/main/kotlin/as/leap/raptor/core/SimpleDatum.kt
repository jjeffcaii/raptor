package `as`.leap.raptor.core

import io.vertx.core.buffer.Buffer

class SimpleDatum : Datum {

  private var wrote: Int = 0
  private val buffer: Buffer

  constructor(alloc: Int) {
    this.buffer = Buffer.buffer(alloc)
  }

  override fun write(buffer: Buffer): Datum {


    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun enough(size: Short): Boolean {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun buffer(): Buffer {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun pop(size: Short): Buffer {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}