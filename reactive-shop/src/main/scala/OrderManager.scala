import java.util.UUID

import akka.actor.{ActorRef, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._

class OrderManager extends Timers {

  override def receive: Receive = initial()

  def initial(): LoggingReceive = {
    case _ => _
  }
}

object OrderManager {

  sealed trait Command

  case class AddItem(id: UUID) extends Command

  case class RemoveItem(id: UUID) extends Command

  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String) extends Command

  case object Buy extends Command

  case object Pay extends Command

  sealed trait Ack

  case object Done extends Ack //trivial ACK

  sealed trait Data

  case class Empty() extends Data

  case class CartData(cartRef: ActorRef) extends Data

  case class CartDataWithSender(cartRef: ActorRef, sender: ActorRef) extends Data

  case class InCheckoutData(checkoutRef: ActorRef) extends Data

  case class InCheckoutDataWithSender(checkoutRef: ActorRef, sender: ActorRef) extends Data

  case class InPaymentData(paymentRef: ActorRef) extends Data

  case class InPaymentDataWithSender(paymentRef: ActorRef, sender: ActorRef) extends Data

}