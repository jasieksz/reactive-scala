import akka.actor.Timers
import akka.event.LoggingReceive
import scala.concurrent.duration._
import Cart._

class Cart(expirationTime: FiniteDuration = 10 seconds) extends Timers {

  override def receive: Receive = empty(0)

  def empty(items: Int): Receive = LoggingReceive {
    case ItemAdded =>
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      context.become(nonEmpty(items + 1))
  }

  def nonEmpty(items: Int): Receive = LoggingReceive {
    case ItemAdded =>
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      context.become(nonEmpty(items + 1))
    case ItemRemoved =>
      if (items == 1) {
        timers.cancel(CartTimerKey)
        context.become(empty(0))
      }
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
      context.become(nonEmpty(items - 1))
    case CartTimerExpired =>
      timers.cancel(CartTimerKey)
      context.become(empty(0))
    case CheckoutStarted =>
      timers.cancel(CartTimerKey)
      context.become(inCheckout(items))
  }

  def inCheckout(items: Int): Receive = LoggingReceive {
    case CheckoutCanceled =>
      context.become(nonEmpty(items))
    case CheckoutClosed =>
      context.become(empty(0))
  }
}

object Cart {

  case object ItemAdded

  case object ItemRemoved

  case object CheckoutStarted

  case object CheckoutCanceled

  case object CheckoutClosed

  case object CartTimerExpired

  case object CartTimerKey

}