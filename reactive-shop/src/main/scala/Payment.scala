import java.util.UUID

import Payment._
import akka.actor.{ActorRef, PoisonPill, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._


class Payment(paymentExpirationTime: FiniteDuration = 10 seconds) extends Timers {

  override def receive: Receive = unnamed()

  def unnamed(): Receive = LoggingReceive {
    case Pay =>
      timers.startSingleTimer(PaymentTimerKey, PaymentTimerExpired, paymentExpirationTime)
      val id = UUID.randomUUID()
      sender() ! PaymentConfirmed(id)
      context.parent ! PaymentReceived(id)

    case OrderManager.CancelPayment | PaymentTimerExpired =>
      sender() ! Cancelled(context.parent)
      context.parent ! Cancelled
  }
}

object Payment {

  sealed trait Command

  case object Pay extends Command

  case class PaymentConfirmed(id: UUID)

  case class PaymentReceived(id: UUID)

  case class Cancelled(checkout: ActorRef)

  case object PaymentTimerExpired

  case object PaymentTimerKey

}