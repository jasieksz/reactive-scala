import Cart._
import akka.actor.FSM
import scala.concurrent.duration._

sealed trait CartState
case object Empty extends CartState
case object NonEmpty extends CartState
case object InCheckout extends CartState

sealed trait CartData
case class CartItems(count: Int = 0) extends CartData

class CartFSM(expirationTime: FiniteDuration) extends FSM[CartState, CartData]{

  startWith(Empty, CartItems())

  when(Empty) {
    case Event(ItemAdded, CartItems(_)) =>
      goto(NonEmpty) using CartItems(1)
  }

  when(NonEmpty, stateTimeout = expirationTime) {
    case Event(ItemRemoved, CartItems(1)) =>
      goto(Empty) using CartItems()

    case Event(ItemRemoved, CartItems(count)) =>
      stay using CartItems(count - 1)

    case Event(ItemAdded, CartItems(count)) =>
      stay using CartItems(count + 1)

    case Event(CheckoutStarted, CartItems(count)) =>
      goto(InCheckout) using CartItems(count)

    case Event(StateTimeout, CartItems(_)) =>
      goto(Empty) using CartItems()

  }

  when(InCheckout) {
    case Event(CheckoutCanceled, CartItems(count)) =>
      goto(NonEmpty) using CartItems(count)

    case Event(CheckoutClosed, CartItems(_)) =>
      goto(Empty) using CartItems()
  }


  initialize()
}

