package managers

import java.net.{HttpRetryException, SocketTimeoutException}
import java.nio.file.AccessDeniedException
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Timers}
import akka.event.LoggingReceive
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import managers.PaymentManager._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


class PaymentManager(checkout: ActorRef, paymentExpirationTime: FiniteDuration = 20 seconds) extends Timers {

  implicit val system: ActorSystem = context.system
  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  var paymentServicePath = "http://localhost:1234/payment"



  override def receive: Receive = unnamed()

  def unnamed(): Receive = LoggingReceive {
    case Pay(replyTo) =>
      timers.startSingleTimer(PaymentTimerKey, PaymentTimerExpired, paymentExpirationTime)
      for {
        response <- Http().singleRequest(HttpRequest(uri = paymentServicePath))
        result <- response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => body.utf8String)
      } yield {
        handleResponse(result, replyTo)
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
    case _ => println()
  }

  private def handleResponse(result: String, replyTo: ActorRef): Unit = {
    result match {
      case "1" =>
        val id = UUID.randomUUID()
        replyTo ! PaymentConfirmed(id)
        checkout ! PaymentReceived(id)

      case "2" =>
        println("BOOM")
        throw new AccessDeniedException("Invalid key")

      case "3" =>
        println("BOOOM")
        throw new SocketTimeoutException("Server not responding")

    }

  }

  override def postRestart(reason: Throwable): Unit = {
    paymentServicePath = "http://localhost:1234/alternative"
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