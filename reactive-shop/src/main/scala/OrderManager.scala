import java.util.UUID

import OrderManager._
import akka.actor.{ActorRef, Props, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._

class OrderManager extends Timers {

  self ! Initialize
  override def receive: Receive = uninitialized()

  def uninitialized(): LoggingReceive = {
    case Initialize =>
      val cart = context.actorOf(Props(new Cart()))
      context.become(withCart(cart))
  }

  def withCart(cartRef: ActorRef) : LoggingReceive = {
    case AddItem(item) =>
      cartRef ! Cart.AddItem(item)

    case RemoveItem(item) =>
      cartRef ! Cart.RemoveItem(item)

    case StartCheckout =>
      cartRef ! Cart.StartCheckout // TODO : How to handle errors : i.e. Empty Cart

    case Cart.CheckoutStarted(checkout) =>
      context.become(withCheckout(checkout))
  }

  def withCheckout(checkoutRef: ActorRef): LoggingReceive = {
    case SelectDeliveryMethod(method) =>
      checkoutRef ! Checkout.SelectDeliveryMethod(method)

    case SelectPaymentMethod(method) =>
      checkoutRef ! Checkout.SelectPaymentMethod(method)

    case CancelCheckout =>
      checkoutRef ! CancelCheckout

    case Checkout.Cancelled(cart) =>
      context.become(withCart(cart))

    case Pay =>
      checkoutRef ! Checkout.Pay // TODO : How to handle errors : i.e. Methods not selected

    case Checkout.PaymentServiceStarted(payment) =>
      context.become(withPayment(payment))
  }

  def withPayment(paymentRef: ActorRef) : LoggingReceive = {
    case Payment.Cancelled(checkout) =>
      context.become(withCheckout(checkout)) // TODO : Does it work? - can checkout remember cartRef (state)?

    case Payment.PaymentConfirmed(id) =>
      context.become(uninitialized())
      self ! Initialize // TODO : In what state this message will be received ?

  }


}

object OrderManager {

  sealed trait Command

  case class AddItem(id: UUID) extends Command

  case class RemoveItem(id: UUID) extends Command

  case class SelectDeliveryMethod(delivery: String) extends Command
  case class SelectPaymentMethod(payment: String) extends Command

  case object Buy extends Command

  case object Pay extends Command

  case object Initialize

  case object StartCheckout

  case object CancelCheckout
}