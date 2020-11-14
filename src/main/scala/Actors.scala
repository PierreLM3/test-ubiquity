import Messages.ConsumerMessage
import Messages.InitMessage
import Messages.MainMessage
import Messages.NewPointMessage
import Messages.ProducerMessage
import Messages.StartMessage
import Messages.StopAll
import Messages.StopConsumerMessage
import Messages.StopProducerMessage
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.MailboxSelector
import akka.actor.typed.scaladsl.Behaviors
import pav.NewPoint
import pav.Pav
import pav.Pav.PavPoints
import pav.PavBin

import scala.reflect.ClassTag

object Messages {
  sealed trait ProducerMessage
  case class StartMessage[A](sendTo: ActorRef[NewPointMessage[A]]) extends ProducerMessage
  case object StopProducerMessage extends ProducerMessage

  sealed trait ConsumerMessage
  case class NewPointMessage[A](point: A) extends ConsumerMessage
  case object StopConsumerMessage extends ConsumerMessage

  sealed trait MainMessage
  case object InitMessage extends MainMessage
  case object StopAll extends MainMessage
}

object Producer {

  def apply[A](numberOfMessagesToSend: Int, pointBuilder: () => A): Behavior[ProducerMessage] = Behaviors.receive { (context, message) =>
    message match {
      case StartMessage(sendTo) => {
        (1 to numberOfMessagesToSend) foreach { _ => sendTo ! NewPointMessage(pointBuilder()) }
        Behaviors.same
      }
      case StopProducerMessage => {
        println("Producer shutdown...")
        context.log.info("Producer shutdown...")
        Behaviors.stopped
      }
    }
  }
}

class DataVector[A: Ordering](maxSize: Int) {
  var v: Vector[A] = Vector[A]()

  def append(a: A): Boolean = {
    if (v.size < maxSize) {
      v = v :+ a
      true
    } else false
  }

  def sortedIt(): Iterator[A] = v.sorted.iterator

  def size: Int = v.size
}

object Consumer {

  def apply[A: PavBin: Ordering: ClassTag](maxSize: Int): Behavior[ConsumerMessage] = {
    newPointBehavior(new DataVector[A](maxSize))
  }

  private def newPointBehavior[A: PavBin: ClassTag](pointsVector: DataVector[A]): Behavior[ConsumerMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case NewPointMessage(point: A) => {
          pointsVector.append(point)
          val pavPoints: PavPoints = Pav.regression(pointsVector.sortedIt)
          println(pavPoints.size)
          Behaviors.same
        }
        case StopConsumerMessage => {
          context.log.info("Consumer shutdown...")
          Behaviors.stopped { () => println("Cleaning up!") }
        }
      }
    }
}

object Main {

  def apply(): Behavior[MainMessage] =
    Behaviors.setup { context =>
      val mailboxSelector = MailboxSelector.fromConfig("my-app.my-mailbox")
      val producerActor = context.spawn(Producer(numberOfMessagesToSend = 100000, pointBuilder = () => NewPoint.random()), "producer")
      val consumerActor = context.spawn(Consumer[NewPoint](maxSize = 100), "consumer", mailboxSelector)

      Behaviors.receiveMessage { message =>
        message match {
          case InitMessage => {
            producerActor ! StartMessage(consumerActor)
            Behaviors.same
          }
          case StopAll => {
            producerActor ! StopProducerMessage
            consumerActor ! StopConsumerMessage
            Behaviors.stopped
          }
        }
      }
    }

  def main(args: Array[String]): Unit = {

    val system: ActorSystem[MainMessage] = ActorSystem(Main(), name = "actorSystem")

    system ! InitMessage

    Thread.sleep(5000)

    system ! StopAll
  }
}
