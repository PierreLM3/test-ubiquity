import Messages.ConsumerMessage
import Messages.InitMessage
import Messages.MainMessage
import Messages.NewPointMessage
import Messages.ProducerMessage
import Messages.StartMessage
import Messages.StopAll
import Messages.StopConsumerMessage
import Messages.StopProducerMessage
import MyVector.VectorWithLimit
import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.MailboxSelector
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSink
import pav.NewPoint
import pav.Pav
import pav.Pav.PavPoints
import pav.PavBin

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
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

object MyVector {

  implicit class VectorWithLimit[A](val self: Vector[A]) {

    def append(a: A, maxSize: Int): Vector[A] = {
      if (self.size < maxSize)
        self :+ a
      else
        self
    }
  }
}

/*class DataVector[A: Ordering](maxSize: Int) {
  var v: Vector[A] = Vector[A]()

  def append(a: A): Boolean = {
    if (v.size < maxSize) {
      v = v :+ a
      true
    } else false
  }

  def sortedIt(): Iterator[A] = v.sorted.iterator

  def size: Int = v.size
}*/

object Consumer {

  def apply[A: PavBin: Ordering: ClassTag](maxSize: Int): Behavior[ConsumerMessage] = {
    newPointBehavior(Vector[A](), maxSize)
  }

  private def newPointBehavior[A: PavBin: ClassTag: Ordering](pointsVector: Vector[A], maxSize: Int): Behavior[ConsumerMessage] =
    Behaviors.receive {
      case (_, NewPointMessage(point: A)) => {
        val newVector: Vector[A] = pointsVector.append(point, maxSize)
        actionWithPoints(newVector)
        newPointBehavior(newVector, maxSize)
      }
      case (context, StopConsumerMessage) => {
        context.log.info("Consumer shutdown...")
        Behaviors.stopped { () => println("Cleaning up!") }
      }
    }

  private def actionWithPoints[A: PavBin: Ordering](pointsVector: Vector[A]) = {
    val pavPoints: PavPoints = Pav.regression(pointsVector.sorted.iterator)
    println(pavPoints.size)
  }
}

object Main {

  def apply(): Behavior[MainMessage] =
    Behaviors.setup { context =>
      val mailboxSelector = MailboxSelector.fromConfig("my-app.my-mailbox")
      //val producerActor = context.spawn(Producer(numberOfMessagesToSend = 100000, pointBuilder = () => NewPoint.random()), "producer")
      val consumerActor = context.spawn(Consumer[NewPoint](maxSize = 100), "consumer", mailboxSelector)

      buildProducer(context, consumerActor)

      Behaviors.receiveMessage {
        case InitMessage => {
          //producerActor ! StartMessage(consumerActor)
          Behaviors.same
        }
        case StopAll => {
          //producerActor ! StopProducerMessage
          consumerActor ! StopConsumerMessage
          Behaviors.stopped
        }
      }
    }

  private def buildProducer(context: ActorContext[MainMessage], consumerActor: ActorRef[ConsumerMessage]): Unit = {
    implicit val materializer = context.system.classicSystem

    val sink: Sink[ConsumerMessage, NotUsed] =
      ActorSink.actorRef[ConsumerMessage](ref = consumerActor, onCompleteMessage = StopConsumerMessage, onFailureMessage = _ => StopConsumerMessage)

    Source(1 to 10000).map(_ => NewPointMessage(NewPoint.random())).runWith(sink)
    ()
  }

  def main(args: Array[String]): Unit = {

    val system: ActorSystem[MainMessage] = ActorSystem(Main(), name = "actorSystem")

    system ! InitMessage

    Thread.sleep(5000)

    system ! StopAll
  }
}
