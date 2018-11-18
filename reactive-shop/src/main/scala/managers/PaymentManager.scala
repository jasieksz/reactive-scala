package managers

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Timers}
import akka.event.LoggingReceive
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import managers.PaymentManager._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global


class PaymentManager(checkout: ActorRef, paymentExpirationTime: FiniteDuration = 10 seconds) extends Timers {

  implicit val system: ActorSystem = context.system

  val paymentServicePath = "localhost:1234/payment"

  override def receive: Receive = unnamed()

  def unnamed(): Receive = LoggingReceive {
    case Pay(replyTo) =>
      timers.startSingleTimer(PaymentTimerKey, PaymentTimerExpired, paymentExpirationTime)
      val id = UUID.randomUUID()

      val response: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = paymentServicePath))
      response onComplete {
        case Success(res) =>
          println(res)
          replyTo ! PaymentConfirmed(id)
          checkout ! PaymentReceived(id)
        case Failure(ex) =>
          ex.printStackTrace()
      }

    case Cancel(replyTo) =>
      replyTo ! PaymentCancelled(checkout)
      checkout ! PaymentCancelled(checkout)
      context.become(cancelled())

    case PaymentTimerExpired =>
      checkout ! PaymentCancelled(checkout)
      context.become(cancelled())
  }

  def cancelled(): Receive = LoggingReceive {
    case _ =>
  }
}

object PaymentManager {

  sealed trait PaymentCommand extends Command

  case class Pay(replyTo: ActorRef) extends PaymentCommand

  case class Cancel(replyTo: ActorRef) extends PaymentCommand

  sealed trait PaymentEvent extends Event

  case class PaymentConfirmed(id: UUID) extends PaymentEvent

  case class PaymentReceived(id: UUID) extends PaymentEvent

  case class PaymentCancelled(checkout: ActorRef) extends PaymentEvent

  case object PaymentTimerExpired extends PaymentEvent

  case object PaymentTimerKey

}