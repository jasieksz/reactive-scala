class Checkout {

}

object Checkout {
  case class DeliveryMethodSelected(method: String)
  case class PaymentSelected(method: String)
  case object PaymentReceived
  case object Cancelled
  case object PaymentTimerExpired
  case object CheckoutTimerExpired
}
