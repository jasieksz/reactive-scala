import java.util.UUID

import akka.actor.{ActorRef, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._
import Cart._

class Cart(expirationTime: FiniteDuration = 10 seconds) extends Timers {

  override def receive: Receive = empty(0)

  def empty(items: Int): Receive = LoggingReceive {
    case AddItem =>
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      context.become(nonEmpty(items + 1))
  }

  def nonEmpty(items: Int): Receive = LoggingReceive {
    case AddItem =>
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      context.become(nonEmpty(items + 1))
    case RemoveItem =>
      if (items == 1) {
        timers.cancel(CartTimerKey)
        context.become(empty(0))
      }
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      context.become(nonEmpty(items - 1))
    case CartTimerExpired =>
      timers.cancel(CartTimerKey)
      context.become(empty(0))
    case StartCheckout =>
      timers.cancel(CartTimerKey)
      context.become(inCheckout(items, sender()))
  }

  def inCheckout(items: Int, actorRef: ActorRef): Receive = LoggingReceive {
    case CheckoutCanceled =>
      context.become(nonEmpty(items))
    case CheckoutClosed =>
      context.become(empty(0))
  }
}

object Cart {

  sealed trait Command

  case class AddItem(id: UUID) extends Command

  case class RemoveItem(id: UUID) extends Command

  case object StartCheckout extends Command

  case class CheckoutStarted(checkoutRef: ActorRef)

  case object CheckoutCanceled

  case object CheckoutClosed

  case object CartTimerExpired

  case object CartTimerKey

}