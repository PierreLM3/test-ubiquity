import Messages.ConsumerMessage
import Messages.MainMessage
import Messages.NewPointMessage
import Messages.StopAll
import Messages.StopConsumerMessage
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

import scala.reflect.ClassTag

object Messages {

  sealed trait ConsumerMessage
  case class NewPointMessage[A](point: A) extends ConsumerMessage
  case object StopConsumerMessage extends ConsumerMessage

  sealed trait MainMessage
  case object StopAll extends MainMessage
}

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
      val consumerActor = context.spawn(Consumer[NewPoint](maxSize = 100), "consumer", mailboxSelector)

      def newPointFactory = () => NewPoint.random()

      buildProducer(context, consumerActor, newPointFactory)

      Behaviors.receiveMessage {
        case StopAll => {
          consumerActor ! StopConsumerMessage
          Behaviors.stopped
        }
      }
    }

  private def buildProducer[A](context: ActorContext[MainMessage], consumerActor: ActorRef[ConsumerMessage], pointFactory: () => A): Unit = {
    implicit val materializer = context.system.classicSystem

    val sink: Sink[ConsumerMessage, NotUsed] =
      ActorSink.actorRef[ConsumerMessage](ref = consumerActor, onCompleteMessage = StopConsumerMessage, onFailureMessage = _ => StopConsumerMessage)

    Source(1 to 10000).map(_ => NewPointMessage(pointFactory())).runWith(sink)
    ()
  }

  def main(args: Array[String]): Unit = {
    val system: ActorSystem[MainMessage] = ActorSystem(Main(), name = "actorSystem")

    Thread.sleep(5000)
    system ! StopAll
  }
}
