package managers

import akka.actor.{ActorRef, PoisonPill, Props, Timers}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import managers.CartManager._
import model.{Cart, Checkout, Item}

import scala.concurrent._
import ExecutionContext.Implicits.global

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class CartManager(expirationTime: FiniteDuration = 10 seconds) extends Timers {

  override def receive: Receive = empty(Cart.empty)

  def empty(cart: Cart): Receive = LoggingReceive {
    case AddItem(item, sender) =>
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      sender ! ItemAdded(item)
      context.become(nonEmpty(cart.addItem(item)))
  }

  def nonEmpty(cart: Cart): Receive = LoggingReceive {
    case AddItem(item, sender) =>
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      context.become(nonEmpty(cart.addItem(item)))

    case RemoveItem(item, count, sender) =>
      val cartCount: Int = cart.getCount(item)
      if (cartCount <= count) {
        timers.cancel(CartTimerKey)
        sender ! ItemRemoved(item, count)
        context.become(empty(Cart.empty))
      } else {
        timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
        sender ! ItemRemoved(item, count)
        context.become(nonEmpty(cart.removeItem(item, count)))
      }

    case StartCheckout(orderManager) =>
      timers.cancel(CartTimerKey)
      val checkout = context.actorOf(Props(new CheckoutManager()))

      implicit val timeout: Timeout = Timeout(1 second)
      checkout ? CheckoutManager.Start(orderManager, self) onComplete {
        case Success(_) =>
          orderManager ! CheckoutStarted(checkout)
          context.become(inCheckout(cart, orderManager))
        case Failure(_) => _
      }

    case CartTimerExpired =>
      timers.cancel(CartTimerKey)
      context.become(empty(Cart.empty))
  }

  def inCheckout(cart: Cart, orderManager: ActorRef): Receive = LoggingReceive {
    case Checkout.Cancelled =>
      context.become(nonEmpty(cart))
    case Checkout.Closed =>
      sender() ! PoisonPill
      context.become(empty(Cart.empty))
  }
}

object CartManager {

  sealed trait CartCommand

  case class AddItem(item: Item, replyTo: ActorRef) extends CartCommand

  case class RemoveItem(item: Item, count: Int, replyTo: ActorRef) extends CartCommand

  case class StartCheckout(replyTo: ActorRef) extends CartCommand

  case object CloseCheckout extends CartCommand

  case object ExpireCartTime extends CartCommand

  case object CancelCheckout extends CartCommand

  sealed trait CartEvent

  case class ItemRemoved(item: Item, count: Int) extends CartEvent

  case class ItemAdded(item: Item) extends CartEvent

  case class CheckoutStarted(checkout: ActorRef) extends CartEvent

  case object CheckoutCancelled extends CartEvent

  case object CheckoutClosed extends CartEvent

  case object CartTimerExpired extends CartEvent

  case object CartTimerKey

}