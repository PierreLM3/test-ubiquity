package mailbox

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.dispatch.AbstractBoundedNodeQueue
import akka.dispatch.BoundedMessageQueueSemantics
import akka.dispatch.Envelope
import akka.dispatch.MailboxType
import akka.dispatch.MessageQueue
import akka.dispatch.MultipleConsumerSemantics
import akka.dispatch.ProducesMessageQueue
import com.typesafe.config.Config

import scala.concurrent.duration.Duration

case class MyNonBlockingBoundedMailbox(capacity: Int) extends MailboxType with ProducesMessageQueue[MyBoundedNodeMessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this(config.getInt("mailbox-capacity"))

  if (capacity < 0) throw new IllegalArgumentException("The capacity for MyNonBlockingBoundedMailbox can not be negative")

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new MyBoundedNodeMessageQueue(capacity)
}

class MyBoundedNodeMessageQueue(capacity: Int)
    extends AbstractBoundedNodeQueue[Envelope](capacity)
    with MessageQueue
    with BoundedMessageQueueSemantics
    with MultipleConsumerSemantics {
  final def pushTimeOut: Duration = Duration.Undefined

  final def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
    if (!add(handle))
      println(s"Lost message ${handle.message}")
  }

  final def dequeue(): Envelope = poll()

  final def numberOfMessages: Int = size()

  final def hasMessages: Boolean = !isEmpty()

  final def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = ()
}
