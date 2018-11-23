package managers

import java.net.HttpRetryException
import java.nio.file.AccessDeniedException

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume}
import akka.actor.{ActorRef, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy, Timers}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, SnapshotOffer}
import managers.CheckoutManager._
import model.Checkout

import scala.concurrent.duration._

class CheckoutManager(val persistence: String,
                      val cart: ActorRef,
                      val orderManager: ActorRef,
                      checkoutExpirationTime: FiniteDuration = 30 seconds) extends Timers with PersistentActor {

  override def persistenceId: String = persistence

  override def receiveCommand: Receive = uninitialized()

  override def receiveRecover: Receive = {
    case event: Event =>
      updateState(event, Checkout.empty)(event => event)

    case SnapshotOffer(_, snapshot: Checkout) =>
      context.become(selectingDeliveryAndPayment(snapshot))
  }


  private def updateState(event: Event, checkout: Checkout)(reply: Event => Unit): Unit = {
    timers.startSingleTimer(CheckoutTimerKey, CheckoutTimerExpired, checkoutExpirationTime)
    reply(event)
    event match {
      case CheckoutStarted(_, _) =>
        context.become(selectingDeliveryAndPayment(Checkout.empty))

      case DeliveryMethodSelected(method) =>
        context.become(selectingDeliveryAndPayment(checkout.selectDelivery(method)))

      case PaymentMethodSelected(method) =>
        context.become(selectingDeliveryAndPayment(checkout.selectPayment(method)))

      case PaymentServiceStarted(_) =>
        timers.cancel(CheckoutTimerKey)
        context.become(processingPayment(checkout))

      case CheckoutCancelled(_) =>
        timers.cancel(CheckoutTimerKey)
        cart ! CheckoutCancelled(cart) // CartManager responds with PoisonPill, should it be persisted?
        context.become(uninitialized())

      case CheckoutTimerExpired =>
        timers.cancel(CheckoutTimerKey)
        context.become(uninitialized())

      case PaymentManager.PaymentReceived(_) =>
        timers.cancel(CheckoutTimerKey)
        context.become(uninitialized())

      case PaymentManager.PaymentCancelled(_) =>
        context.become(selectingDeliveryAndPayment(checkout))
    }
  }

  def uninitialized(): Receive = LoggingReceive {
    case StartCheckout(cartRef, orderManagerRef) =>
      persist(CheckoutStarted(cart, orderManagerRef))(event => updateState(event, Checkout.empty)(_ => cartRef ! CheckoutStarted(self, orderManagerRef)))
  }

  def selectingDeliveryAndPayment(checkout: Checkout): Receive = LoggingReceive {
    case SelectDeliveryMethod(method, replyTo) =>
      saveSnapshot(checkout)
      persist(DeliveryMethodSelected(method))(event => updateState(event, checkout)(event => replyTo ! event))

    case SelectPaymentMethod(method, replyTo) =>
      saveSnapshot(checkout)
      persist(PaymentMethodSelected(method))(event => updateState(event, checkout)(event => replyTo ! event))


    case Buy(replyTo) =>
      if (checkout.isReady()) {
        val paymentActor = context.actorOf(Props(new PaymentManager(self)))
        persist(PaymentServiceStarted(paymentActor))(event => updateState(event, checkout)(event => replyTo ! event))
      }

    case CancelCheckout(replyTo) =>
      persist(CheckoutCancelled(cart))(event => updateState(event, checkout)(event => replyTo ! event))

    case CheckoutTimerExpired =>
      persist(CheckoutTimerExpired)(event => updateState(event, checkout)(_ => {
        cart ! CheckoutCancelled(cart)
        orderManager ! CheckoutCancelled(cart)
      }))

    case "snap" =>
      println("\nSAVING")
      saveSnapshot(checkout)

    case "print" =>
      println("\nCURRENT STATE : " + checkout + "\n")
  }

  def processingPayment(checkout: Checkout): Receive = LoggingReceive {
    case PaymentManager.PaymentReceived(id) =>
      persist(PaymentManager.PaymentReceived(id))(event => updateState(event, checkout)(_ => {
        sender() ! PoisonPill
        cart ! CheckoutClosed
        orderManager ! CheckoutClosed
      }))

    case PaymentManager.PaymentCancelled(checkoutRef) =>
      persist(PaymentManager.PaymentCancelled(checkoutRef))(event => updateState(event, checkout)(_ => sender() ! PoisonPill))
  }

  def cancelled(): Receive = LoggingReceive {
    case _ => println()
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(10, 1 minute) {
    case _: AccessDeniedException => Resume
    case _: HttpRetryException => Restart
    case _: Exception => Escalate
  }
}

object CheckoutManager {

  sealed trait CheckoutCommand extends Command

  case class StartCheckout(cart: ActorRef, orderManager: ActorRef) extends CheckoutCommand

  case class SelectDeliveryMethod(method: String, replyTo: ActorRef) extends CheckoutCommand

  case class SelectPaymentMethod(method: String, replyTo: ActorRef) extends CheckoutCommand

  case class Buy(replyTo: ActorRef) extends CheckoutCommand

  case class CancelCheckout(replyTo: ActorRef) extends CheckoutCommand

  sealed trait CheckoutEvent extends Event

  case class CheckoutStarted(replyTo: ActorRef, orderManager: ActorRef) extends CheckoutEvent

  case class DeliveryMethodSelected(method: String) extends CheckoutEvent

  case class PaymentMethodSelected(method: String) extends CheckoutEvent

  case class PaymentServiceStarted(payment: ActorRef) extends CheckoutEvent

  case class CheckoutCancelled(cart: ActorRef) extends CheckoutEvent

  case object CheckoutClosed extends CheckoutEvent

  case object CheckoutTimerExpired extends CheckoutEvent

  case object CheckoutTimerKey

}
