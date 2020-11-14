import Messages.ConsumerMessage
import Messages.InitMessage
import Messages.MainMessage
import Messages.NewPointMessage
import Messages.ProducerMessage
import Messages.StartMessage
import Messages.StopAll
import Messages.StopConsumerMessage
import Messages.StopProducerMessage
import NewPoint.orderingInstance
import Pav.PavPoints
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Messages {
  sealed trait ProducerMessage
  case class StartMessage(sendTo: ActorRef[NewPointMessage]) extends ProducerMessage
  case object StopProducerMessage extends ProducerMessage

  sealed trait ConsumerMessage
  case class NewPointMessage(point: NewPoint) extends ConsumerMessage
  case object StopConsumerMessage extends ConsumerMessage

  sealed trait MainMessage
  case object InitMessage extends MainMessage
  case object StopAll extends MainMessage
}

object Producer {

  def apply(): Behavior[ProducerMessage] = Behaviors.receive { (context, message) =>
    message match {
      case StartMessage(sendTo) => {
        (1 to 1000) map { _ =>
          sendTo ! NewPointMessage(NewPoint.random())
        }
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

class DataVector(maxSize: Int){
  var v: Vector[NewPoint] = Vector[NewPoint]()

  def append(a: NewPoint) = {
    if (v.size < maxSize) {
      v = v :+ a
    }
  }

  def sortedIt(): Iterator[NewPoint] = v.sorted.iterator
}

object Consumer {

  def apply(): Behavior[ConsumerMessage] = {
    newPointBehavior(new DataVector(10))
  }

  private def newPointBehavior(points: DataVector): Behavior[ConsumerMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case NewPointMessage(point) => {
          points.append(point)
          val points1: PavPoints = Pav.regression[NewPoint](points.sortedIt)
          println(points1.size)
          Behaviors.same
        }
        case StopConsumerMessage => {
          context.log.info("Consumer shutdown...")
          Behaviors.stopped { () =>
            println("Cleaning up!")
          }
        }
      }

    }
}

object Main {

  def apply(): Behavior[MainMessage] =
    Behaviors.setup { context =>
      val producerActor = context.spawn(Producer(), "producer")
      val consumerActor = context.spawn(Consumer(), "consumer")

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
