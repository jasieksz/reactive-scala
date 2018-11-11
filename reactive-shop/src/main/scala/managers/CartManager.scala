package managers

import akka.actor.{ActorRef, PoisonPill, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence.PersistentActor
import managers.CartManager._
import model.{Cart, Item}

import scala.concurrent.duration._

class CartManager(val persistence: String, expirationTime: FiniteDuration = 10 seconds) extends Timers with PersistentActor {

  override def receiveRecover: Receive = empty(Cart.empty)

  override def receiveCommand: Receive = empty(Cart.empty)

  override def persistenceId: String = persistence

  private def updateState(event: Event, cart: Cart)(reply: Event => Unit): Unit = {
    timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
    reply(event)
    event match {
      case ItemAdded(item) =>
        context.become(nonEmpty(cart.addItem(item)))

      case ItemRemoved(item, count) =>
        val cartCount: Int = cart.getCount(item)
        if (count >= cartCount) {
          timers.cancel(CartTimerKey)
          context.become(empty(Cart.empty))
        } else {
          context.become(nonEmpty(cart.removeItems(item, count)))
        }

      case CheckoutStarted(_) =>
        timers.cancel(CartTimerKey)

      case CheckoutManager.Started(_, _) =>
        context.become(inCheckout(cart))

      case CartTimerExpired =>
        timers.cancel(CartTimerKey)
        context.become(empty(Cart.empty))

    }
  }

  def empty(cart: Cart): Receive = LoggingReceive {

    case AddItem(item, replyTo) =>
      persist(ItemAdded(item))(event => updateState(event, cart)(event => replyTo ! event))

    case OrderManager.GetCart(replyTo) =>
      replyTo ! cart
  }

  def nonEmpty(cart: Cart): Receive = LoggingReceive {
    case AddItem(item, replyTo) =>
      persist(ItemAdded(item))(event => updateState(event, cart)(event => replyTo ! event))

    case RemoveItem(item, count, replyTo) =>
      persist(ItemRemoved(item, count))(event => updateState(event, cart)(event => replyTo ! event))

    case StartCheckout(orderManager) =>
      val checkout = context.actorOf(Props(new CheckoutManager()))
      persist(CheckoutStarted(checkout))(event => updateState(event, cart)(_ => checkout ! CheckoutManager.Start(orderManager, self)))

    case CheckoutManager.Started(checkout, orderManager) =>
      persist(CheckoutManager.Started(checkout, orderManager))(event => updateState(event, cart)(_ => orderManager ! CheckoutStarted(checkout)))

    case CartTimerExpired =>
      persist(CartTimerExpired)(event => updateState(event, cart)(event => event))

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

  sealed trait CartCommand extends Command

  case class AddItem(item: Item, replyTo: ActorRef) extends CartCommand

  case class RemoveItem(item: Item, count: Int, replyTo: ActorRef) extends CartCommand

  case class StartCheckout(replyTo: ActorRef) extends CartCommand

  case object CloseCheckout extends CartCommand

  case object ExpireCartTime extends CartCommand

  case object CancelCheckout extends CartCommand

  sealed trait CartEvent extends Event

  case class ItemAdded(item: Item) extends CartEvent

  case class ItemRemoved(item: Item, count: Int) extends CartEvent

  case class CheckoutStarted(checkout: ActorRef) extends CartEvent

  case object CheckoutCancelled extends CartEvent

  case object CheckoutClosed extends CartEvent

  case object CartTimerExpired extends CartEvent

  case object CartTimerKey

}