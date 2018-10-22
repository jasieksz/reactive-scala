import Checkout._
import akka.actor.FSM
import scala.concurrent.duration._

sealed trait CheckoutState
case object SelectingDelivery extends CheckoutState
case object SelectingPaymentMethod extends CheckoutState
case object ProcessingPayment extends CheckoutState
case object Cancelled extends CheckoutState
case object Closed extends CheckoutState

sealed trait CheckoutData
case class CheckoutParameters(delivery: String = "", payment: String = "") extends CheckoutData

class CheckoutFSM(checkoutExpirationTime: FiniteDuration, paymentExpirationTime: FiniteDuration)
  extends FSM[CheckoutState, CheckoutData] {

  startWith(SelectingDelivery, CheckoutParameters())

  when(SelectingDelivery, stateTimeout = checkoutExpirationTime) {
    case Event(DeliveryMethodSelected(method), checkoutData: CheckoutParameters) =>
      goto(SelectingPaymentMethod) using checkoutData.copy(delivery = method)

    case Event(Cancelled | StateTimeout, _) => goto(Cancelled)
  }

  when(SelectingPaymentMethod, stateTimeout = checkoutExpirationTime) {
    case Event(PaymentSelected(payment), checkoutData: CheckoutParameters) =>
      goto(ProcessingPayment) using checkoutData.copy(payment = payment)

    case Event(Cancelled | StateTimeout, _) => goto(Cancelled)
  }

  when(ProcessingPayment, stateTimeout = paymentExpirationTime) {
    case Event(PaymentReceived, _) =>
      goto(Closed)

    case Event(Cancelled | StateTimeout, _) =>
      goto(Cancelled)
  }

  when(Closed) {
    case _ => stop()
  }

  when(Cancelled) {
    case _ => stop()
  }



}
