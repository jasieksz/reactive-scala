import java.util.UUID

import OrderManager._
import akka.actor.{ActorRef, Props, Timers}
import akka.event.LoggingReceive

class OrderManager extends Timers {

  self ! Initialize

  override def receive: Receive = uninitialized()

  def uninitialized(): LoggingReceive = {
    case Initialize =>
      val cart = context.actorOf(Props(new Cart()))
      context.become(withCart(cart))

    case Checkout.Closed =>
      self ! Initialize
  }

  def withCart(cart: ActorRef): LoggingReceive = {
    case AddItem(item) =>
      cart ! Cart.AddItem(item)

    case RemoveItem(item) =>
      cart ! Cart.RemoveItem(item)

    case StartCheckout =>
      cart ! Cart.StartCheckout // TODO : How to handle errors : i.e. Empty Cart

    case Cart.CheckoutStarted(checkout) =>
      context.become(withCheckout(checkout))
  }

  def withCheckout(checkout: ActorRef): LoggingReceive = {
    case SelectDeliveryMethod(method) =>
      checkout ! Checkout.SelectDeliveryMethod(method)

    case SelectPaymentMethod(method) =>
      checkout ! Checkout.SelectPaymentMethod(method)

    case CancelCheckout =>
      checkout ! CancelCheckout

    case Checkout.Cancelled(cart) =>
      context.become(withCart(cart))

    case Buy =>
      checkout ! Checkout.Buy // TODO : How to handle errors : i.e. Methods not selected

    case Checkout.PaymentServiceStarted(payment) =>
      context.become(withPayment(payment))
  }

  def withPayment(payment: ActorRef): LoggingReceive = {

    case Pay =>
      payment ! Payment.Pay

    case Payment.PaymentConfirmed(id) =>
      context.become(uninitialized())

    case CancelPayment =>
      payment ! CancelPayment

    case Payment.Cancelled(checkout) =>
      context.become(withCheckout(checkout)) // TODO : Does it work? - can checkout remember cartRef (state)?
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

  case object CancelPayment

}