package `as`.leap.raptor.core

import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Invoke
import net.engio.mbassy.listener.Listener
import net.engio.mbassy.listener.References

@Listener(references = References.Strong)
class ChunkListener(private val cb: OnChunk) {

  @Handler(delivery = Invoke.Synchronously)
  fun process(msg: Chunk) {
    this.cb(msg)
  }
}