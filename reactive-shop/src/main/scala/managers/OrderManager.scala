package managers

import akka.actor.{ActorRef, Props, Timers}
import akka.event.LoggingReceive
import akka.util.Timeout
import managers.OrderManager._
import model.{Item}

import scala.concurrent.duration._

class OrderManager extends Timers {
  implicit val timeout: Timeout = Timeout(1 seconds)

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

    case RemoveItem(item, count) =>
      cart ! CartManager.RemoveItem(item, count, self)

    case StartCheckout =>
      cart ! CartManager.StartCheckout(self)

    case CartManager.CheckoutStarted(checkout) =>
      context.become(withCheckout(checkout))
  }

  def withCheckout(checkout: ActorRef): Receive = LoggingReceive {
    case SelectDeliveryMethod(method) =>
      checkout ! CheckoutManager.SelectDeliveryMethod(method, self)

    case SelectPaymentMethod(method) =>
      checkout ! CheckoutManager.SelectPaymentMethod(method, self)

    case CancelCheckout =>
      checkout ! CancelCheckout

    case CheckoutManager.Cancelled(cart) =>
      context.become(withCart(cart))

    case Buy =>
      checkout ! CheckoutManager.Buy(self)

    case CheckoutManager.PaymentServiceStarted(payment) =>
      context.become(withPayment(payment))
  }

  def withPayment(payment: ActorRef): Receive = LoggingReceive {

    case Pay =>
      payment ! PaymentManager.Pay(self)

    case PaymentManager.PaymentConfirmed(_) =>
      context.become(uninitialized())
      // TODO : Kill Cart ?

    case CancelPayment =>
      payment ! PaymentManager.Cancel(self)

    case PaymentManager.Cancelled(checkout) =>
      context.become(withCheckout(checkout))
  }
}

object OrderManager {

  sealed trait OrderManagerCommand

  case class AddItem(item: Item) extends OrderManagerCommand

  case class RemoveItem(item: Item, count: Int) extends OrderManagerCommand

  case class SelectDeliveryMethod(method: String) extends OrderManagerCommand

  case class SelectPaymentMethod(method: String) extends OrderManagerCommand

  case object Buy extends OrderManagerCommand

  case object Pay extends OrderManagerCommand

  case object Initialize extends OrderManagerCommand

  case object StartCheckout extends OrderManagerCommand

  case object CancelCheckout extends OrderManagerCommand

  case object CancelPayment extends OrderManagerCommand

  case class GetCart(replyTo: ActorRef) extends OrderManagerCommand

  sealed trait OrderManagerEvent

  case object Done extends OrderManagerEvent
}