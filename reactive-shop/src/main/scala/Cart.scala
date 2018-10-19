class Cart {

}

object Cart {
  case object CartTimerKey
  case object ItemAdded
  case object ItemRemoved
  case object CartTimerExpired
  case object CheckoutStarted
  case object CheckoutCanceled
  case object CheckoutClosed
}