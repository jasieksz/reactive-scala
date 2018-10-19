import Cart.{ItemAdded, ItemRemoved}
import akka.actor.FSM

sealed trait CartState
case object Empty extends CartState
case object NonEmpty extends CartState
case object InCheckout extends CartState

sealed trait CartContent
case class CartItems(count: Int = 0) extends CartContent

class CartFSM extends FSM[CartState, CartContent]{

  startWith(Empty, CartItems())

  when(Empty) {
    case Event(ItemAdded, CartItems(count)) =>
      goto(NonEmpty) using CartItems(count + 1)
  }

  when(NonEmpty) {
    case Event(ItemRemoved, CartItems(1)) =>
      goto(Empty) using CartItems()

    case Event(ItemRemoved, CartItems(count)) =>
      stay using CartItems(count - 1)

    case Event(ItemAdded, CartItems(count)) =>
      stay using CartItems(count + 1)

    // TODO : Add Checkout
    // TODO : Add timeout
  }

  initialize()

}

