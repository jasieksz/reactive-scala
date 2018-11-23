package managers

import akka.actor.{ActorRef, PoisonPill, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, SnapshotOffer}
import managers.CartManager._
import model.{Cart, Item}

import scala.concurrent.duration._

class CartManager(val persistence: String,
                  expirationTime: FiniteDuration = 30 seconds) extends Timers with PersistentActor {

  override def persistenceId: String = persistence

  override def receiveCommand: Receive = uninitialized()

  override def receiveRecover: Receive = {
    case event: Event =>
      updateState(event, Cart.empty)(event => event)
    case SnapshotOffer(_, snapshot: Cart) => {
      if (snapshot.getSize > 0) context.become(nonEmpty(snapshot)) else context.become(empty(Cart.empty))
    }
  }

  private def updateState(event: Event, cart: Cart)(reply: Event => Unit): Unit = {
    timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
    reply(event)
    event match {

      case CartStarted(_) =>
        context.become(empty(Cart.empty))

      case ItemAdded(item) =>
        context.become(nonEmpty(cart.addItem(item)))

      case ItemRemoved(item, count) =>
        val cartCount: Int = cart.getSize()
        if (count >= cartCount) {
          timers.cancel(CartTimerKey)
          context.become(empty(Cart.empty))
        } else {
          context.become(nonEmpty(cart.removeItems(item, count)))
        }

      case CheckoutStarted(_) =>
        timers.cancel(CartTimerKey)

      case CheckoutManager.CheckoutStarted(_, _) =>
        context.become(inCheckout(cart))

      case CartTimerExpired =>
        timers.cancel(CartTimerKey)
        context.become(empty(Cart.empty))
    }
  }

  def uninitialized(): Receive = LoggingReceive {
    case StartCart(replyTo) =>
      persist(CartStarted(self))(event => updateState(event, Cart.empty)(event => replyTo ! event))
  }

  def empty(cart: Cart): Receive = LoggingReceive {

    case AddItem(item, replyTo) =>
      persist(ItemAdded(item))(event => updateState(event, cart)(event => replyTo ! event))

    case OrderManager.GetCart(replyTo) =>
      replyTo ! cart

    case "snap" =>
      println("\nSAVING")
      saveSnapshot(cart)
  }

  def nonEmpty(cart: Cart): Receive = LoggingReceive {
    case AddItem(item, replyTo) =>
      persist(ItemAdded(item))(event => updateState(event, cart)(event => replyTo ! event))

    case RemoveItem(item, count, replyTo) =>
      persist(ItemRemoved(item, count))(event => updateState(event, cart)(event => replyTo ! event))

    case StartCheckout(orderManager) =>
      if (cart.getSize() > 0) {
        saveSnapshot(cart)
        val checkout = context.actorOf(Props(new CheckoutManager(persistence + "checkout", self, orderManager)))
        persist(CheckoutStarted(checkout))(event => updateState(event, cart)(_ => checkout ! CheckoutManager.StartCheckout(self, orderManager)))
      }

    case CheckoutManager.CheckoutStarted(checkout, orderManager) =>
      persist(CheckoutManager.CheckoutStarted(checkout, orderManager))(event => updateState(event, cart)(_ => orderManager ! CheckoutStarted(checkout)))

    case CartTimerExpired =>
      persist(CartTimerExpired)(event => updateState(event, cart)(event => event))

    case OrderManager.GetCart(replyTo) =>
      replyTo ! cart

    case "snap" =>
      println("SAVING\n")
      saveSnapshot(cart)
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case CheckoutManager.CheckoutCancelled =>
      sender() ! PoisonPill
      context.become(nonEmpty(cart))
    case CheckoutManager.CheckoutClosed =>
      sender() ! PoisonPill
      context.become(uninitialized())
      // OM sends PP
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

  case class StartCart(replyTo: ActorRef) extends CartCommand

  sealed trait CartEvent extends Event

  case class ItemAdded(item: Item) extends CartEvent

  case class ItemRemoved(item: Item, count: Int) extends CartEvent

  case class CheckoutStarted(checkout: ActorRef) extends CartEvent

  case object CheckoutCancelled extends CartEvent

  case object CheckoutClosed extends CartEvent

  case class CartStarted(cart: ActorRef) extends CartEvent

  case object CartTimerExpired extends CartEvent

  case object CartTimerKey

}