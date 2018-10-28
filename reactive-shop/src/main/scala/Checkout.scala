import akka.actor.{ActorRef, PoisonPill, Props, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._
import Checkout._

class Checkout(checkoutExpirationTime: FiniteDuration = 10 seconds) extends Timers {

  override def receive: Receive = uninitialized()

  def uninitialized(): Receive = LoggingReceive {
    case Start(orderManager) =>
      context.become(selectingDeliveryAndPayment(orderManager, "", ""))
  }

  def selectingDeliveryAndPayment(orderManager: ActorRef, delivery: String, payment: String): Receive = LoggingReceive {
    case SelectDeliveryMethod(method) =>
      timers.startSingleTimer(CheckoutTimerKey, CheckoutTimerExpired, checkoutExpirationTime)
      context.become(selectingDeliveryAndPayment(orderManager, method, payment))

    case SelectPaymentMethod(method) =>
      timers.startSingleTimer(CheckoutTimerKey, CheckoutTimerExpired, checkoutExpirationTime)
      context.become(selectingDeliveryAndPayment(orderManager, delivery, method))

    case Buy =>
      val paymentActor = context.actorOf(Props(new Payment()))
      orderManager ! PaymentServiceStarted(paymentActor)
      timers.cancel(CheckoutTimerKey)
      context.become(processingPayment(orderManager, delivery, payment))

    case Closed =>
      context.parent ! Closed
      orderManager ! Closed
      self ! PoisonPill

    case CheckoutTimerExpired | OrderManager.CancelCheckout =>
      context.parent ! Cancelled
      orderManager ! Cancelled
      self ! PoisonPill
  }

  def processingPayment(orderManager: ActorRef, delivery: String, payment: String): Receive = LoggingReceive {
    case Payment.PaymentReceived(id) =>
      sender() ! PoisonPill
      context.parent ! Closed
    case Payment.Cancelled =>
      sender() ! PoisonPill
      context.become(selectingDeliveryAndPayment(orderManager, delivery, payment))

  }
}

object Checkout {

  sealed trait Command

  case class Start(orderManagerRef: ActorRef) extends Command

  case class SelectDeliveryMethod(method: String) extends Command

  case class SelectPaymentMethod(method: String) extends Command

  case object Buy

  case class PaymentServiceStarted(paymentRef: ActorRef)

  case class Cancelled(cartRef: ActorRef)

  case object Closed

  case object CheckoutTimerExpired

  case object CheckoutTimerKey

}
