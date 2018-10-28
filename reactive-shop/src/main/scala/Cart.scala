import java.util.UUID

import akka.actor.{ActorRef, PoisonPill, Props, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._
import Cart._

class Cart(expirationTime: FiniteDuration = 10 seconds) extends Timers {

  override def receive: Receive = empty(Map.empty)

  def empty(items: Map[UUID, Int]): Receive = LoggingReceive {
    case AddItem(item) =>
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      val count = items.getOrElse(item, 0)
      context.become(nonEmpty(items updated(item, count + 1)))
  }

  def nonEmpty(items: Map[UUID, Int]): Receive = LoggingReceive {
    case AddItem(item) =>
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      val count = items.getOrElse(item, 0)
      context.become(nonEmpty(items updated(item, count + 1)))

    case RemoveItem(item) =>
      if (items == 1) {
        timers.cancel(CartTimerKey)
        context.become(empty(Map.empty))
      }
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      val count = items.getOrElse(item, 0)
      context.become(nonEmpty(items updated(item, count - 1)))

    case CartTimerExpired =>
      timers.cancel(CartTimerKey)
      context.become(empty(Map.empty))

    case StartCheckout =>
      timers.cancel(CartTimerKey)
      val checkout = context.actorOf(Props(new Checkout()))
      checkout ! Checkout.Start(context.parent)
      sender() ! CheckoutStarted(checkout)
      context.become(inCheckout(items, sender()))
  }

  // TODO : Do I need orderManager?
  def inCheckout(items: Map[UUID, Int], orderManagerRef: ActorRef): Receive = LoggingReceive {
    case Checkout.Cancelled =>
      context.become(nonEmpty(items))
    case Checkout.Closed =>
      sender() ! PoisonPill
      context.become(empty(Map.empty))
  }
}

object Cart {

  sealed trait Command

  case class AddItem(id: UUID) extends Command

  case class RemoveItem(id: UUID) extends Command

  case object StartCheckout extends Command

  case class CheckoutStarted(checkoutRef: ActorRef)

  case object CartTimerExpired

  case object CartTimerKey

}