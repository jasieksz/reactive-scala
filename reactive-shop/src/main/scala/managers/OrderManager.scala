package managers

import java.util.UUID

import akka.actor.{ActorRef, Props, Timers}
import akka.event.LoggingReceive
import managers.OrderManager._
import model.Item

class OrderManager extends Timers {

  self ! Initialize

  override def receive: Receive = uninitialized()

  def uninitialized(): Receive = LoggingReceive {
    case Initialize =>
      val cart = context.actorOf(Props(new CartManager()))
      context.become(withCart(cart))

    case CheckoutManager.Closed =>
      self ! Initialize
  }

  def withCart(cart: ActorRef): Receive = LoggingReceive {
    case AddItem(item) =>
      cart ! CartManager.AddItem(item, self)

    case RemoveItem(item) =>
      cart ! CartManager.RemoveItem(item, self)

    case StartCheckout =>
      cart ! CartManager.StartCheckout

    case CartManager.CheckoutStarted(checkout) =>
      context.become(withCheckout(checkout))
  }

  def withCheckout(checkout: ActorRef): Receive = LoggingReceive {
    case SelectDeliveryMethod(method) =>
      checkout ! CheckoutManager.SelectDeliveryMethod(method)

    case SelectPaymentMethod(method) =>
      checkout ! CheckoutManager.SelectPaymentMethod(method)

    case CancelCheckout =>
      checkout ! CancelCheckout

    case CheckoutManager.Cancelled(cart) =>
      context.become(withCart(cart))

    case Buy =>
      checkout ! CheckoutManager.Buy // TODO : How to handle errors : i.e. Methods not selected

    case CheckoutManager.PaymentServiceStarted(payment) =>
      context.become(withPayment(payment))

//    case GetParametersForTest =>
//      checkout ! managers.Checkout.GetParametersForTest
//
//    case res: (String, String) =>
//      println(res)
  }

  def withPayment(payment: ActorRef): Receive = LoggingReceive {

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

  case class AddItem(item: Item) extends Command

  case class RemoveItem(id: UUID) extends Command

  case class SelectDeliveryMethod(delivery: String) extends Command

  case class SelectPaymentMethod(payment: String) extends Command

  case object Buy extends Command

  case object Pay extends Command

  case object Initialize

  case object StartCheckout

  case object CancelCheckout

  case object CancelPayment

  case object GetParametersForTest

}