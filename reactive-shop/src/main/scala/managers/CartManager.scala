package managers

import akka.actor.{ActorRef, PoisonPill, Props, Timers}
import akka.event.LoggingReceive
import managers.CartManager._
import model.{Cart, Item}


import scala.concurrent.duration._

class CartManager(expirationTime: FiniteDuration = 10 seconds) extends Timers {

  override def receive: Receive = empty(Cart.empty)

  def empty(cart: Cart): Receive = LoggingReceive {
    case AddItem(item, replyTo) =>
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      replyTo ! ItemAdded(item)
      context.become(nonEmpty(cart.addItem(item)))

    case OrderManager.GetCart(replyTo) =>
      replyTo ! cart
  }

  def nonEmpty(cart: Cart): Receive = LoggingReceive {
    case AddItem(item, replyTo) =>
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      replyTo ! ItemAdded(item)
      context.become(nonEmpty(cart.addItem(item)))

    case RemoveItem(item, count, replyTo) =>
      val cartCount: Int = cart.getCount(item)
      if (cartCount <= count) {
        timers.cancel(CartTimerKey)
        replyTo ! ItemRemoved(item, count)
        context.become(empty(Cart.empty))
      } else {
        timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
        replyTo ! ItemRemoved(item, count)
        context.become(nonEmpty(cart.removeItems(item, count)))
      }

    case StartCheckout(orderManager) =>
      timers.cancel(CartTimerKey)
      val checkout = context.actorOf(Props(new CheckoutManager()))
      checkout ! CheckoutManager.Start(orderManager, self)

    case CheckoutManager.Started(checkout, orderManager) =>
      orderManager ! CheckoutStarted(checkout)
      context.become(inCheckout(cart))

    case CartTimerExpired =>
      timers.cancel(CartTimerKey)
      context.become(empty(Cart.empty))

    case OrderManager.GetCart(replyTo) =>
      replyTo ! cart
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case CheckoutManager.Cancelled =>
      sender() ! PoisonPill
      context.become(nonEmpty(cart))
    case CheckoutManager.Closed =>
      sender() ! PoisonPill
      context.become(empty(Cart.empty))
      // TODO : Terminate here or wait for termination from OM
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

  case class ItemAdded(item: Item) extends CartEvent

  case class ItemRemoved(item: Item, count: Int) extends CartEvent

  case class CheckoutStarted(checkout: ActorRef) extends CartEvent

  case object CheckoutCancelled extends CartEvent

  case object CheckoutClosed extends CartEvent

  case object CartTimerExpired extends CartEvent

  case object CartTimerKey

}