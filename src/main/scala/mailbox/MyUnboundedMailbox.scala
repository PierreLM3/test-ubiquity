package mailbox

import java.util.concurrent.ConcurrentLinkedQueue

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.dispatch.Envelope
import akka.dispatch.MailboxType
import akka.dispatch.MessageQueue
import akka.dispatch.ProducesMessageQueue
import com.typesafe.config.Config
import mailbox.MyUnboundedMailbox.MyMessageQueue

trait MyUnboundedMessageQueueSemantics

object MyUnboundedMailbox {
  // This is the MessageQueue implementation
  class MyMessageQueue extends MessageQueue with MyUnboundedMessageQueueSemantics {

    private final val queue = new ConcurrentLinkedQueue[Envelope]()

    def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
      queue.offer(handle)
      ()
    }
    def dequeue(): Envelope = queue.poll()
    def numberOfMessages: Int = queue.size
    def hasMessages: Boolean = !queue.isEmpty
    def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
      while (hasMessages) {
        println( dequeue().message)
      }
    }
  }
}

class MyUnboundedMailbox extends MailboxType with ProducesMessageQueue[MyMessageQueue] {

  import MyUnboundedMailbox._

  def this(settings: ActorSystem.Settings, config: Config) = {
    this()
  }

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = {
    new MyMessageQueue()
  }
}