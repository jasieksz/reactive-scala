class Cart {

}

object Cart {
  case object ItemAdded
  case object ItemRemoved
  case object CheckoutStarted
  case object CheckoutCanceled
  case object CheckoutClosed
  case object CartTimerExpired
}