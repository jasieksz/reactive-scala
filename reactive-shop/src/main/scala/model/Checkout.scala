package model

case class Checkout(delivery: String, payment: String) {

  def selectDelivery(method: String): Checkout = {
    copy(delivery = method)
  }

  def selectPayment(method: String): Checkout = {
    copy(payment = method)
  }

  def isReady(): Boolean = {
    !delivery.equals("") && !payment.equals("")
  }
}

object Checkout {
  val default = Checkout("", "")
}