package `as`.leap.raptor.core

import `as`.leap.raptor.core.model.Message
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Invoke
import net.engio.mbassy.listener.Listener
import net.engio.mbassy.listener.References
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

@Listener(references = References.Weak)
class MessageListener(private val cb: (Message<*>) -> Unit) {

  companion object {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
  }

  @Handler(delivery = Invoke.Synchronously)
  fun process(msg: Message<Any>) {
    if (logger.isDebugEnabled) {
      logger.debug("delivery message: {}", msg)
    }
    this.cb(msg)
  }
}