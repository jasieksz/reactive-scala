import akka.actor.{ActorRef, PoisonPill, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._
import Checkout._

class Checkout (checkoutExpirationTime: FiniteDuration = 10 seconds,
                paymentExpirationTime: FiniteDuration = 10 seconds) extends Timers {

  override def receive: Receive = selectingDelivery("", "")

  def selectingDelivery(delivery: String, payment: String): Receive = LoggingReceive {
    case SelectDeliveryMethod(method) =>
      timers.startSingleTimer(CheckoutTimerKey, CheckoutTimerExpired, checkoutExpirationTime)
      context.become(selectingPaymentMethod(method, payment))
    case CheckoutTimerExpired | Checkout.Cancelled =>
      context.become(cancelled(delivery, payment))
  }

  def selectingPaymentMethod(delivery: String, payment: String): Receive = LoggingReceive {
    case SelectPaymentMethod(method) =>
      timers.cancel(CheckoutTimerKey)
      timers.startSingleTimer(PaymentTimerKey, PaymentTimerExpired, paymentExpirationTime)
      context.become(processingPayment(delivery, method))
    case CheckoutTimerExpired | Checkout.Cancelled =>
      context.become(cancelled(delivery, payment))
  }

  def processingPayment(delivery: String, payment: String): Receive = LoggingReceive {
    case PaymentReceived =>
      timers.cancel(PaymentTimerKey)
      context.become(closed(delivery, payment))
    case PaymentTimerExpired | Checkout.Cancelled =>
      context.become(cancelled(delivery, payment))

  }

  def cancelled(delivery: String, payment: String): Receive = LoggingReceive {
    case _ =>
      println("CANCELLED")
      self ! PoisonPill
  }

  def closed(delivery: String, payment: String): Receive = LoggingReceive {
    case _ =>
      println("CLOSED")
      self ! PoisonPill
  }


}

object Checkout {
  case class SelectDeliveryMethod(method: String = "post")
  case class SelectPaymentMethod(method: String = "credit card")
  case class PaymentServiceStarted(paymentRef: ActorRef)
  case object PaymentReceived
  case object Cancelled
  case object PaymentTimerExpired
  case object CheckoutTimerExpired
  case object PaymentTimerKey
  case object CheckoutTimerKey
}
