import Messages.NewPointMessage
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

// Unused
object Producer {

  sealed trait ProducerMessage
  case class StartMessage[A](sendTo: ActorRef[NewPointMessage[A]]) extends ProducerMessage
  case object StopProducerMessage extends ProducerMessage

  def apply[A](numberOfMessagesToSend: Int, pointBuilder: () => A)(implicit ec: ExecutionContext): Behavior[ProducerMessage] = Behaviors.receive {
    case (_, StartMessage(sendTo)) => {
      Future {
        (1 to numberOfMessagesToSend) foreach { _ => sendTo ! NewPointMessage(pointBuilder()) }
      }
      Behaviors.same
    }
    case (context, StopProducerMessage) => {
      println("Producer shutdown...")
      context.log.info("Producer shutdown...")
      Behaviors.stopped
    }
  }
}
