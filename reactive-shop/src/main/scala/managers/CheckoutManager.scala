package managers

import akka.actor.{ActorRef, PoisonPill, Props, Timers}
import akka.event.LoggingReceive
import managers.CheckoutManager._
import model.Checkout

import scala.concurrent.duration._

class CheckoutManager(checkoutExpirationTime: FiniteDuration = 10 seconds) extends Timers {

  override def receive: Receive = uninitialized()

  def uninitialized(): Receive = LoggingReceive {
    case Start(orderManager, cart) =>
      cart ! Started
      context.become(selectingDeliveryAndPayment(orderManager, cart, Checkout.default))
  }

  def selectingDeliveryAndPayment(orderManager: ActorRef, cart: ActorRef, checkout: Checkout): Receive = LoggingReceive {
    case SelectDeliveryMethod(method, sender) =>
      timers.startSingleTimer(CheckoutTimerKey, CheckoutTimerExpired, checkoutExpirationTime)
      sender ! DeliveryMethodSelected(method)
      context.become(selectingDeliveryAndPayment(orderManager, cart, checkout.selectDelivery(method)))

    case SelectPaymentMethod(method, sender) =>
      timers.startSingleTimer(CheckoutTimerKey, CheckoutTimerExpired, checkoutExpirationTime)
      sender ! PaymentMethodSelected(method)
      context.become(selectingDeliveryAndPayment(orderManager, cart, checkout.selectPayment(method)))

    case Buy(sender) =>
      if (checkout.isReady()) {
        val paymentActor = context.actorOf(Props(new PaymentManager(self)))
        sender ! PaymentServiceStarted(paymentActor)
        timers.cancel(CheckoutTimerKey)
        context.become(processingPayment(orderManager, cart, checkout))
      }

    case Cancel(replyTo) =>
      cart ! Cancelled(cart)
      replyTo ! Cancelled(cart)
      self ! PoisonPill

    case CheckoutTimerExpired =>
      cart ! Cancelled(cart)
      orderManager ! Cancelled(cart)
      self ! PoisonPill
  }

  def processingPayment(orderManager: ActorRef, cart: ActorRef, checkout: Checkout): Receive = LoggingReceive {
    case PaymentManager.PaymentReceived(_) =>
      sender() ! PoisonPill
      cart ! Closed
      orderManager ! Closed
    case PaymentManager.Cancelled =>
      sender() ! PoisonPill
      context.become(selectingDeliveryAndPayment(orderManager, cart, checkout))
  }
}

object CheckoutManager {

  sealed trait CheckoutCommand

  case class Start(orderManager: ActorRef, cart: ActorRef) extends CheckoutCommand

  case class SelectDeliveryMethod(method: String, replyTo: ActorRef) extends CheckoutCommand

  case class SelectPaymentMethod(method: String, replyTo: ActorRef) extends CheckoutCommand

  case class Buy(replyTo: ActorRef) extends CheckoutCommand

  case class Cancel(replyTo: ActorRef) extends CheckoutCommand

  sealed trait CheckoutEvent

  case object Started extends CheckoutEvent

  case class DeliveryMethodSelected(method: String) extends CheckoutEvent

  case class PaymentMethodSelected(method: String) extends CheckoutEvent

  case class PaymentServiceStarted(payment: ActorRef) extends CheckoutEvent

  case class Cancelled(cart: ActorRef) extends CheckoutEvent

  case object Closed extends CheckoutEvent

  case object CheckoutTimerExpired extends CheckoutEvent

  case object CheckoutTimerKey

}
